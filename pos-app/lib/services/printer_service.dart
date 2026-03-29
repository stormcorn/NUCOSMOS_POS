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
    bytes.addAll(
      generator.text('Payment: ${_paymentMethodLabel(receipt.paymentMethod)}'),
    );
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
    if (receipt.discountAmount > 0) {
      bytes.addAll(generator.row([
        PosColumn(
          text: 'Discount',
          width: 8,
          styles: const PosStyles(bold: true),
        ),
        PosColumn(
          text: '-${_currency(receipt.discountAmount)}',
          width: 4,
          styles: const PosStyles(align: PosAlign.right, bold: true),
        ),
      ]));
      final discountTypeLabel = _discountTypeLabel(receipt.discountType);
      if (discountTypeLabel != null) {
        bytes.addAll(generator.text(
          '  類型: $discountTypeLabel',
          styles: const PosStyles(align: PosAlign.left),
        ));
      }
      final discountValueLabel = _discountValueLabel(receipt);
      if (discountValueLabel != null) {
        bytes.addAll(generator.text(
          '  內容: $discountValueLabel',
          styles: const PosStyles(align: PosAlign.left),
        ));
      }
      if (receipt.discountNote?.trim().isNotEmpty ?? false) {
        bytes.addAll(generator.text(
          '  說明: ${receipt.discountNote!.trim()}',
          styles: const PosStyles(align: PosAlign.left),
        ));
      }
    }
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
        text: _paymentMethodLabel(receipt.paymentMethod),
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
    const width = 40;
    return [
      _centerText('NUCOSMOS', width),
      _centerText('系統列印測試頁', width),
      '=' * width,
      _twoColumn('列印時間', _formatDateTime(now), width),
      _twoColumn('列印模式', 'Android 系統列印', width),
      '-' * width,
      '列印測試頁會透過 Android 系統列印介面送出。',
      '請在 Android 列印視窗中選擇可用的印表機。',
      '可搭配 HP、Brother、Canon、Epson 等一般印表機。',
      '',
      '如果要列印門市消費單據，請先完成一筆結帳。',
      '完成後可透過系統列印輸出顧客聯與店家留存聯。',
      '-' * width,
      _centerText('NUCOSMOS POS', width),
    ].join('\n');
  }

  String _buildSystemOrderDocument({
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
  }) {
    return [
      _buildSystemOrderCopy(
        copyLabel: '顧客聯',
        receipt: receipt,
        lines: lines,
        storeCode: storeCode,
        staffName: staffName,
      ),
      _buildSystemOrderCopy(
        copyLabel: '店家留存聯',
        receipt: receipt,
        lines: lines,
        storeCode: storeCode,
        staffName: staffName,
      ),
    ].join('\f');
  }

  String _buildSystemOrderCopy({
    required String copyLabel,
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
  }) {
    const width = 42;
    final printedAt = _formatDateTime(DateTime.now());
    final buffer = StringBuffer()
      ..writeln(_centerText('NUCOSMOS', width))
      ..writeln(_centerText('門市消費單據', width))
      ..writeln(_centerText(copyLabel, width));

    if (storeCode != null && storeCode.trim().isNotEmpty) {
      buffer.writeln(_centerText('門市：$storeCode', width));
    }

    buffer
      ..writeln('=' * width)
      ..writeln(_twoColumn('訂單編號', receipt.orderNumber, width))
      ..writeln(_twoColumn('列印時間', printedAt, width))
      ..writeln(
          _twoColumn('付款方式', _paymentMethodLabel(receipt.paymentMethod), width))
      ..writeln(_twoColumn(
          '付款狀態', _paymentStatusLabel(receipt.paymentStatus), width));

    if (staffName != null && staffName.trim().isNotEmpty) {
      buffer.writeln(_twoColumn('收銀人員', staffName.trim(), width));
    }

    buffer
      ..writeln('-' * width)
      ..writeln(_twoColumn('品項', '金額', width))
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
      ..writeln(_twoColumn('品項數量', '${receipt.itemCount}', width))
      ..writeln(_twoColumn('小計', _currency(receipt.subtotalAmount), width));

    if (receipt.discountAmount > 0) {
      buffer.writeln(
          _twoColumn('優惠', '-${_currency(receipt.discountAmount)}', width));
      final discountTypeLabel = _discountTypeLabel(receipt.discountType);
      if (discountTypeLabel != null) {
        buffer.writeln('優惠類型：$discountTypeLabel');
      }
      final discountValueLabel = _discountValueLabel(receipt);
      if (discountValueLabel != null) {
        buffer.writeln('優惠內容：$discountValueLabel');
      }
      if (receipt.discountNote?.trim().isNotEmpty ?? false) {
        buffer.writeln('優惠說明：${receipt.discountNote!.trim()}');
      }
    }

    buffer
      ..writeln(_twoColumn('合計', _currency(receipt.totalAmount), width))
      ..writeln(_twoColumn('實收', _currency(receipt.paidAmount), width));

    if (receipt.changeAmount > 0) {
      buffer.writeln(_twoColumn('找零', _currency(receipt.changeAmount), width));
    }

    buffer
      ..writeln('=' * width)
      ..writeln(_centerText('感謝您的光臨', width))
      ..writeln(_centerText('NUCOSMOS POS', width));

    return buffer.toString().trimRight();
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
    return '$normalizedLeft\n${' ' * rightIndent}$normalizedRight';
  }

  String _paymentStatusLabel(String value) {
    switch (value.trim().toUpperCase()) {
      case 'PAID':
        return '已付款';
      case 'PENDING':
        return '待付款';
      case 'FAILED':
        return '付款失敗';
      case 'REFUNDED':
        return '已退款';
      default:
        return value;
    }
  }

  String _paymentMethodLabel(String value) {
    switch (value.trim().toUpperCase()) {
      case 'CASH':
        return '現金';
      case 'CARD':
        return '刷卡';
      case 'OTHER':
        return '招待';
      default:
        return value.isEmpty ? '未設定' : value;
    }
  }

  String? _discountTypeLabel(CheckoutDiscountType? type) {
    switch (type) {
      case CheckoutDiscountType.percentage:
        return '折扣';
      case CheckoutDiscountType.amount:
        return '抵用';
      case CheckoutDiscountType.complimentary:
        return '招待';
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
        return '整筆招待';
      case null:
        return null;
    }
  }

  String _trimForPrinter(String value, int maxLength) {
    if (value.length <= maxLength) {
      return value;
    }

    return '${value.substring(0, maxLength - 3)}...';
  }
}
