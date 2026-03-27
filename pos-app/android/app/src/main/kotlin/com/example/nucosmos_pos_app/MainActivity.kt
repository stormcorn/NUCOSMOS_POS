package com.example.nucosmos_pos_app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        private const val CLASSIC_BLUETOOTH_CHANNEL = "nucosmos_pos_app/classic_bluetooth"
        private const val CLASSIC_SCAN_TIMEOUT_MS = 8000L
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CLASSIC_BLUETOOTH_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "scanClassicDevices" -> scanClassicDevices(result)
                else -> result.notImplemented()
            }
        }
    }

    private fun scanClassicDevices(result: MethodChannel.Result) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            result.success(emptyList<Map<String, Any?>>())
            return
        }

        if (!adapter.isEnabled) {
            result.error(
                "BT_DISABLED",
                "請先開啟 Android 藍牙，再重新掃描。",
                null,
            )
            return
        }

        val missingPermission = findMissingClassicBluetoothPermission()
        if (missingPermission != null) {
            result.error(
                "BT_PERMISSION",
                "Android 尚未授權藍牙掃描權限：$missingPermission",
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
            devicesByAddress[address] = current
        }

        try {
            adapter.bondedDevices?.forEach(::collectDevice)
        } catch (securityException: SecurityException) {
            result.error(
                "BT_PERMISSION",
                "讀取已配對藍牙裝置失敗：${securityException.message}",
                null,
            )
            return
        }

        val handler = Handler(Looper.getMainLooper())
        var receiverRegistered = false
        var completed = false

        lateinit var receiver: BroadcastReceiver

        fun finishScan() {
            if (completed) {
                return
            }
            completed = true
            handler.removeCallbacksAndMessages(null)
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
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(
                                    BluetoothDevice.EXTRA_DEVICE,
                                    BluetoothDevice::class.java,
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
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
        } catch (securityException: SecurityException) {
            false
        }

        if (!discoveryStarted) {
            finishScan()
            return
        }

        handler.postDelayed({ finishScan() }, CLASSIC_SCAN_TIMEOUT_MS)
    }

    private fun findMissingClassicBluetoothPermission(): String? {
        val requiredPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions += Manifest.permission.BLUETOOTH_SCAN
            requiredPermissions += Manifest.permission.BLUETOOTH_CONNECT
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requiredPermissions += Manifest.permission.ACCESS_FINE_LOCATION
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requiredPermissions += Manifest.permission.ACCESS_COARSE_LOCATION
        }

        return requiredPermissions.firstOrNull {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun bondStateName(bondState: Int): String =
        when (bondState) {
            BluetoothDevice.BOND_BONDED -> "BONDED"
            BluetoothDevice.BOND_BONDING -> "BONDING"
            else -> "NONE"
        }
}
