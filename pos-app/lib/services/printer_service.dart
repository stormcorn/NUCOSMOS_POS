import 'dart:convert';
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
    String? receiptFooterText,
    bool includeStoreCopy = false,
  }) async {
    final bytes = await _buildOrderReceiptBytes(
      receipt: receipt,
      lines: lines,
      storeCode: storeCode,
      staffName: staffName,
      receiptFooterText: receiptFooterText,
      includeStoreCopy: includeStoreCopy,
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
    String? receiptFooterText,
    bool includeStoreCopy = false,
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
          receiptFooterText: receiptFooterText,
          includeStoreCopy: includeStoreCopy,
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
    String? receiptFooterText,
    bool includeStoreCopy = false,
  }) async {
    final bytes = await _buildOrderReceiptBytes(
      receipt: receipt,
      lines: lines,
      storeCode: storeCode,
      staffName: staffName,
      receiptFooterText: receiptFooterText,
      includeStoreCopy: includeStoreCopy,
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
    String? receiptFooterText,
    bool includeStoreCopy = false,
  }) async {
    final profile = await CapabilityProfile.load();
    final generator = Generator(PaperSize.mm80, profile);
    final bytes = <int>[];
    bytes.addAll(
      _buildThermalOrderCopyBytes(
        generator: generator,
        copyLabel: 'CUSTOMER COPY',
        receipt: receipt,
        lines: lines,
        storeCode: storeCode,
        staffName: staffName,
        receiptFooterText: receiptFooterText,
      ),
    );
    if (includeStoreCopy) {
      bytes.addAll(
        _buildThermalOrderCopyBytes(
          generator: generator,
          copyLabel: 'STORE COPY',
          receipt: receipt,
          lines: lines,
          storeCode: storeCode,
          staffName: staffName,
          receiptFooterText: receiptFooterText,
        ),
      );
    }
    return bytes;
  }

  List<int> _buildThermalOrderCopyBytes({
    required Generator generator,
    required String copyLabel,
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
    String? receiptFooterText,
  }) {
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
      'SALES RECEIPT',
      styles: const PosStyles(align: PosAlign.center, bold: true),
    ));
    bytes.addAll(generator.text(
      copyLabel,
      styles: const PosStyles(align: PosAlign.center, bold: true),
    ));
    bytes.addAll(generator.feed(1));
    if (storeCode != null && storeCode.trim().isNotEmpty) {
      bytes.addAll(generator.text('Store / Store: ${_thermalSafe(storeCode)}'));
    }
    if (staffName != null && staffName.trim().isNotEmpty) {
      bytes.addAll(generator.text('Staff / Clerk: ${_thermalSafe(staffName)}'));
    }
    bytes.addAll(generator.text('Order / No.: ${receipt.orderNumber}'));
    bytes.addAll(
      generator.text(
        'Payment / Pay: ${_thermalPaymentMethodLabel(receipt.paymentMethod)}',
      ),
    );
    bytes.addAll(
      generator.text(
        'Status / State: ${_thermalPaymentStatusLabel(receipt.paymentStatus)}',
      ),
    );
    bytes.addAll(
        generator.text('Printed / Time: ${_formatDateTime(DateTime.now())}'));
    bytes.addAll(generator.hr());
    bytes.addAll(generator.row([
      PosColumn(
        text: 'Item / Prod',
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
          text: _trimForPrinter(_thermalProductLabel(line), 22),
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
          '  - ${_thermalSafe(selection.groupName)}: ${_thermalSafe(selection.optionName)}$deltaText',
          styles: const PosStyles(align: PosAlign.left),
        ));
      }

      bytes.addAll(generator.feed(1));
    }

    bytes.addAll(generator.hr());
    bytes.addAll(generator.row([
      PosColumn(
        text: 'Items / Qty',
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
        text: 'Subtotal / Sub',
        width: 8,
        styles: const PosStyles(bold: true),
      ),
      PosColumn(
        text: _currency(receipt.subtotalAmount),
        width: 4,
        styles: const PosStyles(align: PosAlign.right, bold: true),
      ),
    ]));
    if (receipt.discountAmount > 0) {
      bytes.addAll(generator.row([
        PosColumn(
          text: 'Discount / Off',
          width: 8,
          styles: const PosStyles(bold: true),
        ),
        PosColumn(
          text: '-${_currency(receipt.discountAmount)}',
          width: 4,
          styles: const PosStyles(align: PosAlign.right, bold: true),
        ),
      ]));
      final discountTypeLabel = _thermalDiscountTypeLabel(receipt.discountType);
      if (discountTypeLabel != null) {
        bytes.addAll(generator.text(
          '  Type / Kind: ${_thermalSafe(discountTypeLabel)}',
          styles: const PosStyles(align: PosAlign.left),
        ));
      }
      final discountValueLabel = _thermalDiscountValueLabel(receipt);
      if (discountValueLabel != null) {
        bytes.addAll(generator.text(
          '  Value / Amt: ${_thermalSafe(discountValueLabel)}',
          styles: const PosStyles(align: PosAlign.left),
        ));
      }
      if (receipt.discountNote?.trim().isNotEmpty ?? false) {
        bytes.addAll(generator.text(
          '  Note / Memo: ${_thermalSafe(receipt.discountNote!.trim())}',
          styles: const PosStyles(align: PosAlign.left),
        ));
      }
    }
    bytes.addAll(generator.row([
      PosColumn(
        text: 'Total / Due',
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
        text: 'Paid / Recv',
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
          text: 'Change / Back',
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
        text: 'Payment / Way',
        width: 8,
        styles: const PosStyles(bold: true),
      ),
      PosColumn(
        text: _thermalPaymentMethodLabel(receipt.paymentMethod),
        width: 4,
        styles: const PosStyles(align: PosAlign.right, bold: true),
      ),
    ]));
    final redeemCode = receipt.redeemCode?.trim();
    final redeemUrl = receipt.redeemUrl?.trim();
    if ((redeemCode?.isNotEmpty ?? false) || (redeemUrl?.isNotEmpty ?? false)) {
      bytes.addAll(generator.hr());
      bytes.addAll(generator.text(
        'Redeem / Lucky Draw',
        styles: const PosStyles(align: PosAlign.center, bold: true),
      ));
      if (redeemCode != null && redeemCode.isNotEmpty) {
        bytes.addAll(generator.text('Code / No.: ${_thermalSafe(redeemCode)}'));
      }
      if (redeemUrl != null && redeemUrl.isNotEmpty) {
        bytes.addAll(
          generator.text(
            _thermalSafe(redeemUrl, fallback: ''),
            styles: const PosStyles(align: PosAlign.center),
          ),
        );
        bytes.addAll(generator.feed(1));
        try {
          bytes.addAll(generator.qrcode(redeemUrl));
        } catch (_) {
          bytes.addAll(
            generator.text(
              'Scan redeem URL online.',
              styles: const PosStyles(align: PosAlign.center),
            ),
          );
        }
      }
    }
    bytes.addAll(generator.feed(1));
    bytes.addAll(generator.text(
      'Thank you / Thanks',
      styles: const PosStyles(align: PosAlign.center, bold: true),
    ));
    bytes.addAll(generator.text(
      'NUCOSMOS POS',
      styles: const PosStyles(align: PosAlign.center),
    ));
    final normalizedFooter = _normalizeReceiptFooterText(receiptFooterText);
    if (normalizedFooter.isNotEmpty) {
      bytes.addAll(generator.feed(1));
      bytes.addAll(generator.hr());
      for (final line in normalizedFooter.split('\n')) {
        final safeLine = _thermalSafe(line, fallback: '');
        if (safeLine.trim().isNotEmpty) {
          bytes.addAll(
            generator.text(
              safeLine,
              styles: const PosStyles(align: PosAlign.center),
            ),
          );
        }
      }
    }
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
    const width = 40;
    return [
      _centerText('NUCOSMOS', width),
      _centerText('\u5370\u8868\u6a5f\u6e2c\u8a66\u9801', width),
      '=' * width,
      _twoColumn('\u5217\u5370\u6642\u9593', _formatDateTime(now), width),
      _twoColumn('\u5217\u5370\u985e\u578b', 'Android \u7cfb\u7d71\u5217\u5370',
          width),
      '-' * width,
      '\u9019\u662f Android \u7cfb\u7d71\u5217\u5370\u7684\u6e2c\u8a66\u9801\u3002',
      '\u5982\u679c\u756b\u9762\u6709\u8df3\u51fa\u7cfb\u7d71\u5217\u5370\u9078\u55ae\uff0c\u4ee3\u8868\u4e00\u822c\u5370\u8868\u6a5f\u5217\u5370\u5165\u53e3\u6b63\u5e38\u3002',
      '\u53ef\u642d\u914d HP\u3001Brother\u3001Canon\u3001Epson \u7b49\u4e00\u822c\u5370\u8868\u6a5f\u6e2c\u8a66\u3002',
      '',
      '\u82e5\u4f60\u8981\u5217\u5370 POS \u71b1\u611f\u6536\u64da\uff0c\u8acb\u6539\u7528\u4e0a\u65b9\u7684\u71b1\u611f\u6a5f\u6383\u63cf\u8207\u6e2c\u8a66\u6309\u9215\u3002',
      '\u7cfb\u7d71\u5217\u5370\u9069\u5408\u5e97\u5bb6\u7559\u5b58\u806f\u3001\u4e00\u822c\u8fa6\u516c\u5ba4\u5370\u8868\u6a5f\u8207 A4 \u55ae\u64da\u3002',
      '-' * width,
      _centerText('NUCOSMOS POS', width),
    ].join('\\n');
  }

  String _buildSystemOrderDocument({
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
    String? receiptFooterText,
    bool includeStoreCopy = false,
  }) {
    final copies = <String>[
      _buildSystemOrderCopy(
        copyLabel: '\u9867\u5ba2\u806f Customer Copy',
        receipt: receipt,
        lines: lines,
        storeCode: storeCode,
        staffName: staffName,
        receiptFooterText: receiptFooterText,
      ),
    ];
    if (includeStoreCopy) {
      copies.add(
        _buildSystemOrderCopy(
          copyLabel: '\u5e97\u5bb6\u7559\u5b58\u806f Store Copy',
          receipt: receipt,
          lines: lines,
          storeCode: storeCode,
          staffName: staffName,
          receiptFooterText: receiptFooterText,
        ),
      );
    }
    return copies.join('\\f');
  }

  String _buildSystemOrderCopy({
    required String copyLabel,
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
    String? receiptFooterText,
  }) {
    const width = 42;
    final printedAt = _formatDateTime(DateTime.now());
    final buffer = StringBuffer()
      ..writeln(_centerText('NUCOSMOS', width))
      ..writeln(_centerText(
          '\u9580\u5e02\u6d88\u8cbb\u55ae\u64da Sales Receipt', width))
      ..writeln(_centerText(copyLabel, width));

    if (storeCode != null && storeCode.trim().isNotEmpty) {
      buffer.writeln(_centerText('\u9580\u5e02 Store\uff1a$storeCode', width));
    }

    buffer
      ..writeln('=' * width)
      ..writeln(_twoColumn(
          '\u8a02\u55ae\u7de8\u865f Order', receipt.orderNumber, width))
      ..writeln(
          _twoColumn('\u5217\u5370\u6642\u9593 Printed', printedAt, width))
      ..writeln(_twoColumn('\u4ed8\u6b3e\u65b9\u5f0f Payment',
          _paymentMethodLabel(receipt.paymentMethod), width))
      ..writeln(_twoColumn('\u4ed8\u6b3e\u72c0\u614b Status',
          _paymentStatusLabel(receipt.paymentStatus), width));

    if (staffName != null && staffName.trim().isNotEmpty) {
      buffer.writeln(_twoColumn(
          '\u6536\u9280\u4eba\u54e1 Staff', staffName.trim(), width));
    }

    buffer
      ..writeln('-' * width)
      ..writeln(_twoColumn('\u5546\u54c1 Product', '\u91d1\u984d Total', width))
      ..writeln('-' * width);

    for (final line in lines) {
      buffer.writeln(
        _twoColumn(
          '${line.product.name} x${line.quantity}',
          _currency(line.lineTotal),
          width,
        ),
      );
      buffer.writeln('  ${_currency(line.unitPrice)} x ${line.quantity}');
      for (final selection in line.selectedOptions) {
        final deltaText = selection.priceDelta > 0
            ? ' (+${_currency(selection.priceDelta)})'
            : '';
        buffer.writeln(
          '  + ${selection.groupName}: ${selection.optionName}$deltaText',
        );
      }
      buffer.writeln();
    }

    buffer
      ..writeln('-' * width)
      ..writeln(_twoColumn(
          '\u54c1\u9805\u6578\u91cf Items', '${receipt.itemCount}', width))
      ..writeln(_twoColumn(
          '\u5c0f\u8a08 Subtotal', _currency(receipt.subtotalAmount), width));

    if (receipt.discountAmount > 0) {
      buffer.writeln(_twoColumn('\u512a\u60e0 Discount',
          '-${_currency(receipt.discountAmount)}', width));
      final discountTypeLabel = _discountTypeLabel(receipt.discountType);
      if (discountTypeLabel != null) {
        buffer.writeln(
            '\u512a\u60e0\u985e\u578b Discount Type\uff1a$discountTypeLabel');
      }
      final discountValueLabel = _discountValueLabel(receipt);
      if (discountValueLabel != null) {
        buffer.writeln(
            '\u512a\u60e0\u5167\u5bb9 Discount Value\uff1a$discountValueLabel');
      }
      if (receipt.discountNote?.trim().isNotEmpty ?? false) {
        buffer.writeln(
            '\u512a\u60e0\u8aaa\u660e Discount Note\uff1a${receipt.discountNote!.trim()}');
      }
    }

    buffer
      ..writeln(_twoColumn(
          '\u5408\u8a08 Total', _currency(receipt.totalAmount), width))
      ..writeln(_twoColumn(
          '\u5be6\u6536 Paid', _currency(receipt.paidAmount), width));

    if (receipt.changeAmount > 0) {
      buffer.writeln(_twoColumn(
          '\u627e\u96f6 Change', _currency(receipt.changeAmount), width));
    }

    if ((receipt.redeemCode?.trim().isNotEmpty ?? false) ||
        (receipt.redeemUrl?.trim().isNotEmpty ?? false)) {
      buffer
        ..writeln('-' * width)
        ..writeln(_centerText(
            '\u5168\u7db2\u514c\u734e / \u6703\u54e1\u5165\u53e3 Redeem',
            width));
      if (receipt.redeemCode?.trim().isNotEmpty ?? false) {
        buffer.writeln(_twoColumn(
            '\u514c\u734e\u78bc Code', receipt.redeemCode!.trim(), width));
      }
      if (receipt.redeemUrl?.trim().isNotEmpty ?? false) {
        buffer.writeln('URL: ${receipt.redeemUrl!.trim()}');
      }
    }

    buffer
      ..writeln('=' * width)
      ..writeln(
          _centerText('\u611f\u8b1d\u60a8\u7684\u5149\u81e8 Thank You', width))
      ..writeln(_centerText('NUCOSMOS POS', width));

    final normalizedFooter = _normalizeReceiptFooterText(receiptFooterText);
    if (normalizedFooter.isNotEmpty) {
      buffer
        ..writeln('-' * width)
        ..writeln(_centerText('\u9580\u5e02\u5099\u8a3b Footer', width));
      for (final line in normalizedFooter.split('\\n')) {
        final trimmedLine = line.trim();
        if (trimmedLine.isNotEmpty) {
          buffer.writeln(trimmedLine);
        }
      }
    }

    return buffer.toString().trimRight();
  }

  String _normalizeReceiptFooterText(String? value) {
    if (value == null) {
      return '';
    }
    return value.replaceAll('\\r\\n', '\\n').trim();
  }

  String _centerText(String text, int width) {
    if (text.length >= width) {
      return text;
    }

    final leftPadding = ((width - text.length) / 2).floor();
    return '${' ' * leftPadding}$text';
  }

  String _twoColumn(String left, String right, int width) {
    final normalizedLeft = left.trim();
    final normalizedRight = right.trim();
    final spacing = width - normalizedLeft.length - normalizedRight.length;
    if (spacing >= 1) {
      return '$normalizedLeft${' ' * spacing}$normalizedRight';
    }

    final rightIndent = (width - normalizedRight.length).clamp(0, width);
    return '$normalizedLeft\\n${' ' * rightIndent}$normalizedRight';
  }

  String _paymentStatusLabel(String value) {
    switch (value.trim().toUpperCase()) {
      case 'PAID':
        return '\u5df2\u4ed8\u6b3e';
      case 'PENDING':
        return '\u5f85\u4ed8\u6b3e';
      case 'FAILED':
        return '\u4ed8\u6b3e\u5931\u6557';
      case 'REFUNDED':
        return '\u5df2\u9000\u6b3e';
      default:
        return value;
    }
  }

  String _paymentMethodLabel(String value) {
    switch (value.trim().toUpperCase()) {
      case 'CASH':
        return '\u73fe\u91d1';
      case 'CARD':
        return '\u5237\u5361';
      case 'OTHER':
        return '\u62db\u5f85';
      default:
        return value.isEmpty ? '\u672a\u6307\u5b9a' : value;
    }
  }

  String? _discountTypeLabel(CheckoutDiscountType? type) {
    switch (type) {
      case CheckoutDiscountType.percentage:
        return '\u6298\u6263';
      case CheckoutDiscountType.amount:
        return '\u62b5\u7528';
      case CheckoutDiscountType.complimentary:
        return '\u62db\u5f85';
      case null:
        return null;
    }
  }

  String? _discountValueLabel(OrderReceipt receipt) {
    switch (receipt.discountType) {
      case CheckoutDiscountType.percentage:
        if (receipt.discountValue == null) {
          return null;
        }
        final value = receipt.discountValue!;
        return '${value.toStringAsFixed(value == value.roundToDouble() ? 0 : 2)}%';
      case CheckoutDiscountType.amount:
        if (receipt.discountValue == null) {
          return null;
        }
        return _currency(receipt.discountValue!);
      case CheckoutDiscountType.complimentary:
        return '\u6574\u7b46\u62db\u5f85';
      case null:
        return null;
    }
  }

  String? _thermalDiscountTypeLabel(CheckoutDiscountType? type) {
    switch (type) {
      case CheckoutDiscountType.percentage:
        return 'PERCENT';
      case CheckoutDiscountType.amount:
        return 'AMOUNT';
      case CheckoutDiscountType.complimentary:
        return 'COMPLIMENTARY';
      case null:
        return null;
    }
  }

  String? _thermalDiscountValueLabel(OrderReceipt receipt) {
    switch (receipt.discountType) {
      case CheckoutDiscountType.percentage:
        if (receipt.discountValue == null) {
          return null;
        }
        final value = receipt.discountValue!;
        return '${value.toStringAsFixed(value == value.roundToDouble() ? 0 : 2)}%';
      case CheckoutDiscountType.amount:
        if (receipt.discountValue == null) {
          return null;
        }
        return _currency(receipt.discountValue!);
      case CheckoutDiscountType.complimentary:
        return 'FULL ORDER';
      case null:
        return null;
    }
  }

  String _thermalPaymentStatusLabel(String value) {
    switch (value.trim().toUpperCase()) {
      case 'PAID':
        return 'PAID';
      case 'PENDING':
        return 'PENDING';
      case 'FAILED':
        return 'FAILED';
      case 'REFUNDED':
        return 'REFUNDED';
      default:
        return _thermalSafe(value, fallback: 'UNKNOWN');
    }
  }

  String _thermalPaymentMethodLabel(String value) {
    switch (value.trim().toUpperCase()) {
      case 'CASH':
        return 'CASH';
      case 'CARD':
        return 'CARD';
      case 'OTHER':
        return 'COMPLIMENTARY';
      default:
        return _thermalSafe(value, fallback: 'UNKNOWN');
    }
  }

  String _thermalProductLabel(PosCartLine line) {
    final safeName = _thermalSafe(line.product.name, fallback: '');
    final collapsed = safeName.replaceAll('?', '').trim();
    if (collapsed.isNotEmpty) {
      return safeName;
    }
    final sku = line.product.sku.trim();
    if (sku.isNotEmpty) {
      return _thermalSafe(sku, fallback: 'ITEM');
    }
    return 'ITEM';
  }

  String _thermalSafe(String value, {String fallback = '?'}) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) {
      return fallback;
    }

    final ascii = String.fromCharCodes(
      trimmed.runes.map((rune) {
        if (rune == 9 || rune == 10 || rune == 13) {
          return rune;
        }
        if (rune >= 32 && rune <= 126) {
          return rune;
        }
        return 63;
      }),
    );

    final normalized = ascii.replaceAll(RegExp(r'\?{2,}'), '?').trim();
    return normalized.isEmpty ? fallback : normalized;
  }

  String _trimForPrinter(String value, int maxLength) {
    if (value.length <= maxLength) {
      return value;
    }

    return '${value.substring(0, maxLength - 3)}...';
  }
}
