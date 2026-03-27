import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_thermal_printer/flutter_thermal_printer.dart';
import 'package:flutter_thermal_printer/utils/printer.dart';
import 'package:flutter/services.dart';

import '../models/classic_bluetooth_device.dart';
import '../models/classic_bluetooth_status.dart';
import '../models/order_models.dart';
import '../state/session_controller.dart';

class PrinterService {
  PrinterService() {
    _plugin.bleConfig = const BleConfig(
      connectionStabilizationDelay: Duration(seconds: 3),
    );
  }

  static const MethodChannel _classicBluetoothChannel = MethodChannel(
    'nucosmos_pos_app/classic_bluetooth',
  );
  static const MethodChannel _androidPrintChannel = MethodChannel(
    'nucosmos_pos_app/android_print',
  );

  final FlutterThermalPrinter _plugin = FlutterThermalPrinter.instance;

  Stream<List<Printer>> get devicesStream => _plugin.devicesStream;

  Future<List<ClassicBluetoothDevice>> scanClassicBluetoothDevices() async {
    final result = await _classicBluetoothChannel.invokeMethod<List<dynamic>>(
      'scanClassicDevices',
    );

    if (result == null) {
      return const [];
    }

    return result
        .whereType<Map>()
        .map(ClassicBluetoothDevice.fromMap)
        .toList(growable: false);
  }

  Future<ClassicBluetoothStatus> getClassicBluetoothStatus() async {
    final result =
        await _classicBluetoothChannel.invokeMapMethod<String, dynamic>(
              'getClassicStatus',
            ) ??
            const <String, dynamic>{};
    return ClassicBluetoothStatus.fromMap(result);
  }

  Future<ClassicBluetoothStatus> requestClassicBluetoothPermissions() async {
    final result =
        await _classicBluetoothChannel.invokeMapMethod<String, dynamic>(
              'requestClassicPermissions',
            ) ??
            const <String, dynamic>{};
    return ClassicBluetoothStatus.fromMap(result);
  }

  Future<void> openClassicBluetoothSettings() async {
    await _classicBluetoothChannel.invokeMethod('openBluetoothSettings');
  }

  Future<void> connectClassicBluetoothDevice(
    ClassicBluetoothDevice device,
  ) async {
    await _classicBluetoothChannel.invokeMethod(
      'connectClassicDevice',
      <String, dynamic>{'address': device.address},
    );
  }

  Future<void> disconnectClassicBluetoothDevice() async {
    await _classicBluetoothChannel.invokeMethod('disconnectClassicDevice');
  }

  Future<void> printClassicTestReceipt(
    ClassicBluetoothDevice device,
  ) async {
    final bytes = await _buildTestReceiptBytes();
    await _printClassicBytes(device, bytes);
  }

  Future<void> printClassicOrderReceipt({
    required ClassicBluetoothDevice device,
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
  }) async {
    final bytes = await _buildOrderReceiptBytes(
      receipt: receipt,
      lines: lines,
      storeCode: storeCode,
      staffName: staffName,
    );
    await _printClassicBytes(device, bytes);
  }

  Future<void> printSystemTestDocument() async {
    final now = DateTime.now();
    await _androidPrintChannel.invokeMethod(
      'printSystemDocument',
      <String, dynamic>{
        'title': 'NUCOSMOS POS Test Page',
        'content': _buildSystemTestDocument(now),
      },
    );
  }

  Future<void> printSystemOrderDocument({
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
  }) async {
    await _androidPrintChannel.invokeMethod(
      'printSystemDocument',
      <String, dynamic>{
        'title': 'NUCOSMOS Order ${receipt.orderNumber}',
        'content': _buildSystemOrderDocument(
          receipt: receipt,
          lines: lines,
          storeCode: storeCode,
          staffName: staffName,
        ),
      },
    );
  }

  Future<void> startDiscovery() async {
    await _plugin.getPrinters(
      connectionTypes: const [ConnectionType.USB, ConnectionType.BLE],
    );
  }

  void stopDiscovery() {
    _plugin.stopScan();
  }

  Future<void> connect(Printer printer) async {
    await _plugin.connect(printer);
  }

  Future<void> disconnect(Printer printer) async {
    await _plugin.disconnect(printer);
  }

  Future<void> printTestReceipt(Printer printer) async {
    final bytes = await _buildTestReceiptBytes();
    await _plugin.printData(printer, bytes, longData: true);
  }

  Future<void> printOrderReceipt({
    required Printer printer,
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
  }) async {
    final bytes = await _buildOrderReceiptBytes(
      receipt: receipt,
      lines: lines,
      storeCode: storeCode,
      staffName: staffName,
    );
    await _plugin.printData(printer, bytes, longData: true);
  }

  Future<void> _printClassicBytes(
    ClassicBluetoothDevice device,
    List<int> bytes,
  ) async {
    await _classicBluetoothChannel.invokeMethod(
      'printClassicBytes',
      <String, dynamic>{
        'address': device.address,
        'bytes': Uint8List.fromList(bytes),
      },
    );
  }

  Future<List<int>> _buildTestReceiptBytes() async {
    final profile = await CapabilityProfile.load();
    final generator = Generator(PaperSize.mm80, profile);
    final now = DateTime.now();

    final bytes = <int>[];
    bytes.addAll(generator.text(
      'NUCOSMOS POS',
      styles: const PosStyles(
        align: PosAlign.center,
        bold: true,
        height: PosTextSize.size2,
        width: PosTextSize.size2,
      ),
    ));
    bytes.addAll(generator.text(
      'Printer Test Receipt',
      styles: const PosStyles(align: PosAlign.center, bold: true),
    ));
    bytes.addAll(generator.hr());
    bytes.addAll(generator.text('Time: ${_formatDateTime(now)}'));
    bytes.addAll(generator.text('Check: Bluetooth / USB thermal printer'));
    bytes.addAll(generator.feed(1));
    bytes.addAll(generator.text(
      'If this page prints correctly, the printer connection is ready.',
      styles: const PosStyles(align: PosAlign.left),
    ));
    bytes.addAll(generator.feed(2));
    bytes.addAll(generator.text(
      'NUCOSMOS',
      styles: const PosStyles(align: PosAlign.center, bold: true),
    ));
    bytes.addAll(generator.feed(2));
    bytes.addAll(generator.cut());
    return bytes;
  }

  Future<List<int>> _buildOrderReceiptBytes({
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
  }) async {
    final profile = await CapabilityProfile.load();
    final generator = Generator(PaperSize.mm80, profile);
    final bytes = <int>[];

    bytes.addAll(generator.text(
      'NUCOSMOS POS',
      styles: const PosStyles(
        align: PosAlign.center,
        bold: true,
        height: PosTextSize.size2,
        width: PosTextSize.size2,
      ),
    ));
    bytes.addAll(generator.text(
      'Sales Receipt',
      styles: const PosStyles(align: PosAlign.center, bold: true),
    ));
    bytes.addAll(generator.feed(1));
    if (storeCode != null && storeCode.trim().isNotEmpty) {
      bytes.addAll(generator.text('Store: $storeCode'));
    }
    if (staffName != null && staffName.trim().isNotEmpty) {
      bytes.addAll(generator.text('Staff: $staffName'));
    }
    bytes.addAll(generator.text('Order: ${receipt.orderNumber}'));
    bytes.addAll(generator.text('Payment: CASH'));
    bytes.addAll(generator.text('Status: ${receipt.paymentStatus}'));
    bytes.addAll(generator.text('Printed: ${_formatDateTime(DateTime.now())}'));
    bytes.addAll(generator.hr());
    bytes.addAll(generator.row([
      PosColumn(
        text: 'Item',
        width: 6,
        styles: const PosStyles(bold: true),
      ),
      PosColumn(
        text: 'Qty',
        width: 2,
        styles: const PosStyles(align: PosAlign.center, bold: true),
      ),
      PosColumn(
        text: 'Total',
        width: 4,
        styles: const PosStyles(align: PosAlign.right, bold: true),
      ),
    ]));
    bytes.addAll(generator.hr());

    for (final line in lines) {
      bytes.addAll(generator.row([
        PosColumn(
          text: _trimForPrinter(line.product.name, 22),
          width: 6,
        ),
        PosColumn(
          text: '${line.quantity}',
          width: 2,
          styles: const PosStyles(align: PosAlign.center),
        ),
        PosColumn(
          text: _currency(line.lineTotal),
          width: 4,
          styles: const PosStyles(align: PosAlign.right),
        ),
      ]));
      bytes.addAll(generator.text(
        '  ${_currency(line.unitPrice)} x ${line.quantity}',
        styles: const PosStyles(align: PosAlign.left),
      ));

      for (final selection in line.selectedOptions) {
        final deltaText = selection.priceDelta > 0
            ? ' (+${_currency(selection.priceDelta)})'
            : '';
        bytes.addAll(generator.text(
          '  - ${selection.groupName}: ${selection.optionName}$deltaText',
          styles: const PosStyles(align: PosAlign.left),
        ));
      }

      bytes.addAll(generator.feed(1));
    }

    bytes.addAll(generator.hr());
    bytes.addAll(generator.row([
      PosColumn(
        text: 'Items',
        width: 8,
        styles: const PosStyles(bold: true),
      ),
      PosColumn(
        text: '${receipt.itemCount}',
        width: 4,
        styles: const PosStyles(align: PosAlign.right, bold: true),
      ),
    ]));
    bytes.addAll(generator.row([
      PosColumn(
        text: 'Subtotal',
        width: 8,
        styles: const PosStyles(bold: true),
      ),
      PosColumn(
        text: _currency(receipt.subtotalAmount),
        width: 4,
        styles: const PosStyles(align: PosAlign.right, bold: true),
      ),
    ]));
    bytes.addAll(generator.row([
      PosColumn(
        text: 'Total',
        width: 8,
        styles: const PosStyles(
          bold: true,
          height: PosTextSize.size2,
          width: PosTextSize.size2,
        ),
      ),
      PosColumn(
        text: _currency(receipt.totalAmount),
        width: 4,
        styles: const PosStyles(
          align: PosAlign.right,
          bold: true,
          height: PosTextSize.size2,
          width: PosTextSize.size2,
        ),
      ),
    ]));
    bytes.addAll(generator.row([
      PosColumn(
        text: 'Paid',
        width: 8,
        styles: const PosStyles(bold: true),
      ),
      PosColumn(
        text: _currency(receipt.paidAmount),
        width: 4,
        styles: const PosStyles(align: PosAlign.right, bold: true),
      ),
    ]));
    if (receipt.changeAmount > 0) {
      bytes.addAll(generator.row([
        PosColumn(
          text: 'Change',
          width: 8,
          styles: const PosStyles(bold: true),
        ),
        PosColumn(
          text: _currency(receipt.changeAmount),
          width: 4,
          styles: const PosStyles(align: PosAlign.right, bold: true),
        ),
      ]));
    }
    bytes.addAll(generator.row([
      PosColumn(
        text: 'Payment',
        width: 8,
        styles: const PosStyles(bold: true),
      ),
      PosColumn(
        text: 'Cash',
        width: 4,
        styles: const PosStyles(align: PosAlign.right, bold: true),
      ),
    ]));
    bytes.addAll(generator.feed(1));
    bytes.addAll(generator.text(
      'Thank you for your order',
      styles: const PosStyles(align: PosAlign.center, bold: true),
    ));
    bytes.addAll(generator.text(
      'NUCOSMOS POS',
      styles: const PosStyles(align: PosAlign.center),
    ));
    bytes.addAll(generator.feed(2));
    bytes.addAll(generator.cut());
    return bytes;
  }

  String encodeTarget(Map<String, dynamic> json) => jsonEncode(json);

  Map<String, dynamic> decodeTarget(String value) =>
      (jsonDecode(value) as Map).cast<String, dynamic>();

  String _formatDateTime(DateTime value) {
    String two(int number) => number.toString().padLeft(2, '0');
    return '${value.year}-${two(value.month)}-${two(value.day)} '
        '${two(value.hour)}:${two(value.minute)}';
  }

  String _currency(double value) => '\$${value.toStringAsFixed(2)}';

  String _buildSystemTestDocument(DateTime now) {
    return [
      'NUCOSMOS POS',
      'Android System Print Test',
      '',
      'Printed: ${_formatDateTime(now)}',
      'Mode: Android Print Framework',
      '',
      'This page is for standard printers such as HP, Brother, Canon, and Epson.',
      'If this page prints correctly, the Android printer entry is working.',
      '',
      '---',
      'NUCOSMOS',
    ].join('\n');
  }

  String _buildSystemOrderDocument({
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
  }) {
    final buffer = StringBuffer()
      ..writeln('NUCOSMOS POS')
      ..writeln('Sales Receipt')
      ..writeln();

    if (storeCode != null && storeCode.trim().isNotEmpty) {
      buffer.writeln('Store: $storeCode');
    }
    if (staffName != null && staffName.trim().isNotEmpty) {
      buffer.writeln('Staff: $staffName');
    }
    buffer
      ..writeln('Order: ${receipt.orderNumber}')
      ..writeln('Payment: CASH')
      ..writeln('Status: ${receipt.paymentStatus}')
      ..writeln('Printed: ${_formatDateTime(DateTime.now())}')
      ..writeln()
      ..writeln('Items');

    for (final line in lines) {
      buffer.writeln(
        '- ${line.product.name}  x${line.quantity}  ${_currency(line.lineTotal)}',
      );
      buffer.writeln('  ${_currency(line.unitPrice)} x ${line.quantity}');
      for (final selection in line.selectedOptions) {
        final deltaText = selection.priceDelta > 0
            ? ' (+${_currency(selection.priceDelta)})'
            : '';
        buffer.writeln(
          '  ${selection.groupName}: ${selection.optionName}$deltaText',
        );
      }
    }

    buffer
      ..writeln()
      ..writeln('Summary')
      ..writeln('Items: ${receipt.itemCount}')
      ..writeln('Subtotal: ${_currency(receipt.subtotalAmount)}')
      ..writeln('Total: ${_currency(receipt.totalAmount)}')
      ..writeln('Paid: ${_currency(receipt.paidAmount)}');

    if (receipt.changeAmount > 0) {
      buffer.writeln('Change: ${_currency(receipt.changeAmount)}');
    }

    buffer
      ..writeln()
      ..writeln('Thank you for your order')
      ..writeln('NUCOSMOS POS');

    return buffer.toString().trimRight();
  }

  String _trimForPrinter(String value, int maxLength) {
    if (value.length <= maxLength) {
      return value;
    }

    return '${value.substring(0, maxLength - 3)}...';
  }
}
