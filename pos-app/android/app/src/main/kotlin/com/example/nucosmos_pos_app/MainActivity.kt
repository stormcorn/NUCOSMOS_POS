package com.example.nucosmos_pos_app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : FlutterActivity() {
    companion object {
        private const val CLASSIC_BLUETOOTH_CHANNEL = "nucosmos_pos_app/classic_bluetooth"
        private const val ANDROID_PRINT_CHANNEL = "nucosmos_pos_app/android_print"
        private const val CLASSIC_SCAN_TIMEOUT_MS = 8000L
        private const val REQUEST_CLASSIC_PERMISSIONS = 2207
        private val CLASSIC_SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private var classicSocket: BluetoothSocket? = null
    private var classicConnectedAddress: String? = null
    private var pendingPermissionResult: MethodChannel.Result? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CLASSIC_BLUETOOTH_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "scanClassicDevices" -> scanClassicDevices(result)
                "getClassicStatus" -> result.success(buildClassicStatus())
                "requestClassicPermissions" -> requestClassicPermissions(result)
                "openBluetoothSettings" -> {
                    openBluetoothSettings()
                    result.success(true)
                }

                "connectClassicDevice" -> {
                    val address = call.argument<String>("address")
                    if (address.isNullOrBlank()) {
                        result.error("BT_ADDRESS", "Bluetooth device address is required.", null)
                    } else {
                        connectClassicDevice(address, result)
                    }
                }

                "disconnectClassicDevice" -> {
                    disconnectClassicDevice()
                    result.success(true)
                }

                "printClassicBytes" -> {
                    val address = call.argument<String>("address")
                    val bytes = call.argument<ByteArray>("bytes")
                    if (address.isNullOrBlank() || bytes == null || bytes.isEmpty()) {
                        result.error("BT_PRINT", "Classic Bluetooth print arguments are incomplete.", null)
                    } else {
                        printClassicBytes(address, bytes, result)
                    }
                }

                else -> result.notImplemented()
            }
        }

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            ANDROID_PRINT_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "printSystemDocument" -> {
                    val title = call.argument<String>("title")
                    val content = call.argument<String>("content")
                    if (title.isNullOrBlank() || content.isNullOrBlank()) {
                        result.error(
                            "ANDROID_PRINT",
                            "System print title and content are required.",
                            null,
                        )
                    } else {
                        printSystemDocument(title, content)
                        result.success(true)
                    }
                }

                else -> result.notImplemented()
            }
        }
    }

    override fun onDestroy() {
        disconnectClassicDevice()
        ioExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CLASSIC_PERMISSIONS) {
            return
        }

        val result = pendingPermissionResult
        pendingPermissionResult = null
        result?.success(buildClassicStatus())
    }

    private fun scanClassicDevices(result: MethodChannel.Result) {
        val adapter = bluetoothAdapter()
        if (adapter == null) {
            result.success(emptyList<Map<String, Any?>>())
            return
        }

        if (!adapter.isEnabled) {
            result.error("BT_DISABLED", "Android Bluetooth is turned off.", null)
            return
        }

        val missingPermission = findMissingClassicBluetoothPermission()
        if (missingPermission != null) {
            result.error(
                "BT_PERMISSION",
                "Android is missing Bluetooth permission: $missingPermission",
                null,
            )
            return
        }

        val devicesByAddress = linkedMapOf<String, MutableMap<String, Any?>>()

        fun collectDevice(device: BluetoothDevice?) {
            if (device == null) {
                return
            }

            val address = device.address ?: return
            val current = devicesByAddress[address] ?: mutableMapOf()
            current["name"] = device.name ?: ""
            current["address"] = address
            current["bondState"] = bondStateName(device.bondState)
            current["isConnected"] = address == classicConnectedAddress
            devicesByAddress[address] = current
        }

        try {
            adapter.bondedDevices?.forEach(::collectDevice)
        } catch (securityException: SecurityException) {
            result.error(
                "BT_PERMISSION",
                "Failed to read paired Bluetooth devices: ${securityException.message}",
                null,
            )
            return
        }

        var receiverRegistered = false
        var completed = false

        lateinit var receiver: BroadcastReceiver

        fun finishScan() {
            if (completed) {
                return
            }
            completed = true
            mainHandler.removeCallbacksAndMessages(null)
            if (receiverRegistered) {
                try {
                    unregisterReceiver(receiver)
                } catch (_: IllegalArgumentException) {
                }
            }
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            result.success(devicesByAddress.values.toList())
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = bluetoothDeviceFromIntent(intent)
                        collectDevice(device)
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> finishScan()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true

        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }

        val discoveryStarted = try {
            adapter.startDiscovery()
        } catch (_: SecurityException) {
            false
        }

        if (!discoveryStarted) {
            finishScan()
            return
        }

        mainHandler.postDelayed({ finishScan() }, CLASSIC_SCAN_TIMEOUT_MS)
    }

    private fun requestClassicPermissions(result: MethodChannel.Result) {
        val missingPermissions = findMissingClassicBluetoothPermissions()
        if (missingPermissions.isEmpty()) {
            result.success(buildClassicStatus())
            return
        }

        pendingPermissionResult = result
        ActivityCompat.requestPermissions(
            this,
            missingPermissions.toTypedArray(),
            REQUEST_CLASSIC_PERMISSIONS,
        )
    }

    private fun openBluetoothSettings() {
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    private fun printSystemDocument(title: String, content: String) {
        runOnUiThread {
            val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                return@runOnUiThread
            }

            val attributes = PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(
                title,
                PlainTextPrintDocumentAdapter(context = this, title = title, content = content),
                attributes,
            )
        }
    }

    private fun connectClassicDevice(address: String, result: MethodChannel.Result) {
        val adapter = bluetoothAdapter()
        if (adapter == null) {
            result.error("BT_UNAVAILABLE", "Bluetooth adapter is not available.", null)
            return
        }

        if (!adapter.isEnabled) {
            result.error("BT_DISABLED", "Android Bluetooth is turned off.", null)
            return
        }

        val missingPermission = findMissingClassicBluetoothPermission()
        if (missingPermission != null) {
            result.error("BT_PERMISSION", "Missing Bluetooth permission: $missingPermission", null)
            return
        }

        ioExecutor.execute {
            try {
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }

                val device = adapter.getRemoteDevice(address)
                disconnectClassicDevice()

                val socket = createClassicSocket(device)
                socket.connect()
                classicSocket = socket
                classicConnectedAddress = address
                postSuccess(result, true)
            } catch (exception: Exception) {
                disconnectClassicDevice()
                postError(result, "BT_CONNECT", "Classic Bluetooth connection failed: ${exception.message}")
            }
        }
    }

    private fun disconnectClassicDevice() {
        try {
            classicSocket?.close()
        } catch (_: IOException) {
        } finally {
            classicSocket = null
            classicConnectedAddress = null
        }
    }

    private fun printClassicBytes(
        address: String,
        bytes: ByteArray,
        result: MethodChannel.Result,
    ) {
        val adapter = bluetoothAdapter()
        if (adapter == null) {
            result.error("BT_UNAVAILABLE", "Bluetooth adapter is not available.", null)
            return
        }

        ioExecutor.execute {
            try {
                if (classicConnectedAddress != address || classicSocket?.isConnected != true) {
                    val device = adapter.getRemoteDevice(address)
                    disconnectClassicDevice()
                    val socket = createClassicSocket(device)
                    socket.connect()
                    classicSocket = socket
                    classicConnectedAddress = address
                }

                val outputStream = classicSocket?.outputStream
                    ?: throw IOException("Classic Bluetooth output stream is unavailable.")
                outputStream.write(bytes)
                outputStream.flush()
                postSuccess(result, true)
            } catch (exception: Exception) {
                postError(result, "BT_PRINT", "Classic Bluetooth print failed: ${exception.message}")
            }
        }
    }

    private fun createClassicSocket(device: BluetoothDevice): BluetoothSocket {
        return try {
            device.createRfcommSocketToServiceRecord(CLASSIC_SPP_UUID)
        } catch (exception: Exception) {
            @Suppress("UNCHECKED_CAST")
            val fallbackMethod = device.javaClass.getMethod(
                "createRfcommSocket",
                Int::class.javaPrimitiveType,
            )
            fallbackMethod.invoke(device, 1) as BluetoothSocket
        }
    }

    private fun buildClassicStatus(): Map<String, Any?> {
        val adapter = bluetoothAdapter()
        val bondedCount = try {
            adapter?.bondedDevices?.size ?: 0
        } catch (_: SecurityException) {
            0
        }

        return mapOf(
            "bluetoothEnabled" to (adapter?.isEnabled == true),
            "missingPermissions" to findMissingClassicBluetoothPermissions(),
            "bondedDeviceCount" to bondedCount,
            "connectedAddress" to classicConnectedAddress,
        )
    }

    private fun bluetoothAdapter(): BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private fun findMissingClassicBluetoothPermission(): String? =
        findMissingClassicBluetoothPermissions().firstOrNull()

    private fun findMissingClassicBluetoothPermissions(): List<String> {
        val requiredPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions += Manifest.permission.BLUETOOTH_SCAN
            requiredPermissions += Manifest.permission.BLUETOOTH_CONNECT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requiredPermissions += Manifest.permission.ACCESS_FINE_LOCATION
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requiredPermissions += Manifest.permission.ACCESS_COARSE_LOCATION
        }

        return requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun bluetoothDeviceFromIntent(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun bondStateName(bondState: Int): String {
        return when (bondState) {
            BluetoothDevice.BOND_BONDED -> "BONDED"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            else -> "NONE"
        }
    }

    private fun postSuccess(result: MethodChannel.Result, value: Any?) {
        mainHandler.post { result.success(value) }
    }

    private fun postError(result: MethodChannel.Result, code: String, message: String) {
        mainHandler.post { result.error(code, message, null) }
    }

    private class PlainTextPrintDocumentAdapter(
        private val context: Context,
        private val title: String,
        private val content: String,
    ) : PrintDocumentAdapter() {
        private var printAttributes: PrintAttributes? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: android.os.CancellationSignal,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            printAttributes = newAttributes
            if (cancellationSignal.isCanceled) {
                callback.onLayoutCancelled()
                return
            }

            val info = PrintDocumentInfo.Builder(title)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: android.os.CancellationSignal,
            callback: WriteResultCallback,
        ) {
            val attributes = printAttributes
                ?: PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
            val pdfDocument = PrintedPdfDocument(context, attributes)
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                typeface = Typeface.MONOSPACE
                color = android.graphics.Color.BLACK
            }
            val titlePaint = Paint(textPaint).apply {
                textSize = 16f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            val lineHeight = textPaint.fontSpacing + 4f
            val titleHeight = titlePaint.fontSpacing + 16f
            val horizontalPadding = 36f
            val topPadding = 40f
            val bottomPadding = 40f

            try {
                var pageNumber = 0
                val sections = content
                    .replace("\r\n", "\n")
                    .split('\u000C')
                    .ifEmpty { listOf("") }

                for ((sectionIndex, section) in sections.withIndex()) {
                    val bodyLines = section.split('\n').ifEmpty { listOf("") }
                    var lineIndex = 0
                    var wrappedLineOffset = 0

                    while (lineIndex < bodyLines.size) {
                        if (cancellationSignal.isCanceled) {
                            callback.onWriteCancelled()
                            pdfDocument.close()
                            destination.close()
                            return
                        }

                        val page = pdfDocument.startPage(pageNumber)
                        val canvas: Canvas = page.canvas
                        val pageHeight = page.info.pageHeight.toFloat()
                        val pageWidth = page.info.pageWidth.toFloat()
                        val usableWidth = pageWidth - horizontalPadding * 2
                        val maxBodyY = pageHeight - bottomPadding

                        var y = topPadding
                        if (pageNumber == 0 || lineIndex == 0) {
                            val header = if (sectionIndex == 0) title else "$title (${sectionIndex + 1})"
                            canvas.drawText(header, horizontalPadding, y, titlePaint)
                            y += titleHeight
                        }

                        while (lineIndex < bodyLines.size && y <= maxBodyY) {
                            val wrappedLines = wrapLine(bodyLines[lineIndex], textPaint, usableWidth)
                            while (wrappedLineOffset < wrappedLines.size && y <= maxBodyY) {
                                canvas.drawText(
                                    wrappedLines[wrappedLineOffset],
                                    horizontalPadding,
                                    y,
                                    textPaint,
                                )
                                y += lineHeight
                                wrappedLineOffset += 1
                            }

                            if (wrappedLineOffset >= wrappedLines.size) {
                                lineIndex += 1
                                wrappedLineOffset = 0
                            }
                        }

                        pdfDocument.finishPage(page)
                        pageNumber += 1
                    }
                }

                pdfDocument.writeTo(ParcelFileDescriptor.AutoCloseOutputStream(destination))
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (exception: Exception) {
                callback.onWriteFailed(exception.message)
            } finally {
                pdfDocument.close()
            }
        }

        private fun wrapLine(
            line: String,
            paint: Paint,
            maxWidth: Float,
        ): List<String> {
            if (line.isEmpty()) {
                return listOf("")
            }

            val wrapped = mutableListOf<String>()
            var current = StringBuilder()

            for (character in line) {
                val candidate = current.toString() + character
                if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                    current.append(character)
                } else {
                    wrapped += current.toString()
                    current = StringBuilder(character.toString())
                }
            }

            if (current.isNotEmpty()) {
                wrapped += current.toString()
            }

            return wrapped
        }

    }
}
