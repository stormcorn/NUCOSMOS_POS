import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_thermal_printer/utils/printer.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/order_models.dart';
import '../models/printer_target.dart';
import '../services/printer_service.dart';
import 'session_controller.dart';

class PrinterController extends ChangeNotifier {
  PrinterController({required PrinterService printerService})
      : _printerService = printerService;

  static const _selectedPrinterKey = 'pos.selected_printer';
  static const _autoPrintEnabledKey = 'pos.auto_print_receipt';

  final PrinterService _printerService;

  StreamSubscription<List<Printer>>? _devicesSubscription;
  List<Printer> printers = const [];
  Printer? selectedPrinter;
  PrinterTarget? rememberedPrinter;

  bool restoring = true;
  bool scanning = false;
  bool connecting = false;
  bool printing = false;
  bool autoPrintReceipt = true;

  String statusMessage = '';
  String errorMessage = '';

  bool get hasConnectedPrinter => selectedPrinter?.isConnected ?? false;

  Future<void> restoreSettings() async {
    restoring = true;
    notifyListeners();

    final prefs = await SharedPreferences.getInstance();
    final storedPrinter = prefs.getString(_selectedPrinterKey);
    autoPrintReceipt = prefs.getBool(_autoPrintEnabledKey) ?? true;

    if (storedPrinter != null && storedPrinter.isNotEmpty) {
      try {
        rememberedPrinter = PrinterTarget.fromJson(
          _printerService.decodeTarget(storedPrinter),
        );
      } catch (_) {
        rememberedPrinter = null;
      }
    }

    _devicesSubscription ??=
        _printerService.devicesStream.listen(_handlePrinterUpdate);

    restoring = false;
    notifyListeners();
  }

  Future<void> startScan() async {
    errorMessage = '';
    statusMessage =
        '\u6b63\u5728\u6383\u63cf\u9644\u8fd1\u7684\u85cd\u7259 BLE / USB \u5370\u8868\u6a5f...';
    scanning = true;
    notifyListeners();

    try {
      await _printerService.startDiscovery();
    } catch (error) {
      scanning = false;
      errorMessage = '\u6383\u63cf\u5931\u6557\uff1a$error';
      notifyListeners();
    }
  }

  Future<void> stopScan() async {
    _printerService.stopDiscovery();
    scanning = false;
    if (statusMessage ==
        '\u6b63\u5728\u6383\u63cf\u9644\u8fd1\u7684\u85cd\u7259 BLE / USB \u5370\u8868\u6a5f...') {
      statusMessage = '\u5df2\u505c\u6b62\u6383\u63cf\u3002';
    }
    notifyListeners();
  }

  Future<void> connectPrinter(Printer printer) async {
    connecting = true;
    errorMessage = '';
    statusMessage =
        '\u6b63\u5728\u9023\u7dda\u5230 ${_printerLabel(printer)}...';
    notifyListeners();

    try {
      if ((selectedPrinter?.isConnected ?? false) &&
          !_isSamePrinter(selectedPrinter!, printer)) {
        await _printerService.disconnect(selectedPrinter!);
      }

      await _printerService.connect(printer);
      selectedPrinter = _mergePrinter(printer, connected: true);
      rememberedPrinter = _toTarget(selectedPrinter!);
      statusMessage =
          '\u5df2\u9023\u7dda\u5230 ${_printerLabel(selectedPrinter!)}';
      await _persistPrinterSelection();
    } catch (error) {
      errorMessage = '\u9023\u7dda\u5931\u6557\uff1a$error';
    } finally {
      connecting = false;
      notifyListeners();
    }
  }

  Future<void> disconnectSelectedPrinter() async {
    final printer = selectedPrinter;
    if (printer == null) {
      return;
    }

    connecting = true;
    notifyListeners();

    try {
      await _printerService.disconnect(printer);
      selectedPrinter = _mergePrinter(printer, connected: false);
      statusMessage = '\u5df2\u4e2d\u65b7 ${_printerLabel(printer)}';
    } catch (error) {
      errorMessage = '\u4e2d\u65b7\u9023\u7dda\u5931\u6557\uff1a$error';
    } finally {
      connecting = false;
      notifyListeners();
    }
  }

  Future<void> printTestReceipt() async {
    final printer = await _ensureSelectedPrinterConnected();
    if (printer == null) {
      return;
    }

    printing = true;
    errorMessage = '';
    statusMessage = '\u6b63\u5728\u5217\u5370\u6e2c\u8a66\u9801...';
    notifyListeners();

    try {
      await _printerService.printTestReceipt(printer);
      statusMessage = '\u6e2c\u8a66\u9801\u5217\u5370\u5b8c\u6210\u3002';
    } catch (error) {
      errorMessage = '\u6e2c\u8a66\u5217\u5370\u5931\u6557\uff1a$error';
    } finally {
      printing = false;
      notifyListeners();
    }
  }

  Future<void> printReceiptForOrder({
    required OrderReceipt receipt,
    required List<PosCartLine> lines,
    String? storeCode,
    String? staffName,
  }) async {
    if (!autoPrintReceipt) {
      return;
    }

    final printer = await _ensureSelectedPrinterConnected();
    if (printer == null) {
      return;
    }

    printing = true;
    errorMessage = '';
    statusMessage =
        '\u6b63\u5728\u5217\u5370\u8a02\u55ae ${receipt.orderNumber}...';
    notifyListeners();

    try {
      await _printerService.printOrderReceipt(
        printer: printer,
        receipt: receipt,
        lines: lines,
        storeCode: storeCode,
        staffName: staffName,
      );
      statusMessage =
          '\u8a02\u55ae ${receipt.orderNumber} \u5df2\u5217\u5370\u3002';
    } catch (error) {
      errorMessage = '\u8a02\u55ae\u5217\u5370\u5931\u6557\uff1a$error';
    } finally {
      printing = false;
      notifyListeners();
    }
  }

  Future<void> setAutoPrintReceipt(bool value) async {
    autoPrintReceipt = value;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_autoPrintEnabledKey, value);
    notifyListeners();
  }

  void _handlePrinterUpdate(List<Printer> devices) {
    printers = devices.where((printer) {
      final name = (printer.name ?? '').trim();
      final address = (printer.address ?? '').trim();
      final vendorId = (printer.vendorId ?? '').trim();
      final productId = (printer.productId ?? '').trim();
      return name.isNotEmpty ||
          address.isNotEmpty ||
          (vendorId.isNotEmpty && productId.isNotEmpty);
    }).toList(growable: false);

    scanning = false;
    _refreshSelectedPrinterFromDevices();
    if (printers.isEmpty) {
      statusMessage =
          '\u5c1a\u672a\u627e\u5230\u53ef\u7528\u7684\u85cd\u7259 / USB \u5370\u8868\u6a5f\u3002';
    } else {
      statusMessage =
          '\u5df2\u627e\u5230 ${printers.length} \u53f0\u5370\u8868\u6a5f\uff0c\u8acb\u9078\u64c7\u8981\u9023\u7dda\u7684\u88dd\u7f6e\u3002';
    }
    notifyListeners();
  }

  void _refreshSelectedPrinterFromDevices() {
    if (selectedPrinter != null) {
      final match = printers.cast<Printer?>().firstWhere(
            (printer) =>
                printer != null && _isSamePrinter(printer, selectedPrinter!),
            orElse: () => null,
          );
      if (match != null) {
        selectedPrinter = match;
        rememberedPrinter = _toTarget(match);
      }
      return;
    }

    if (rememberedPrinter == null) {
      return;
    }

    final match = printers.cast<Printer?>().firstWhere(
          (printer) =>
              printer != null &&
              _matchesRememberedPrinter(printer, rememberedPrinter!),
          orElse: () => null,
        );
    if (match != null) {
      selectedPrinter = match;
      statusMessage =
          '\u5df2\u627e\u5230\u5148\u524d\u914d\u5c0d\u7684\u5370\u8868\u6a5f\uff1a${_printerLabel(match)}';
    }
  }

  Future<void> _persistPrinterSelection() async {
    final prefs = await SharedPreferences.getInstance();
    final target = rememberedPrinter;
    if (target == null) {
      await prefs.remove(_selectedPrinterKey);
      return;
    }

    await prefs.setString(
      _selectedPrinterKey,
      _printerService.encodeTarget(target.toJson()),
    );
  }

  Future<Printer?> _ensureSelectedPrinterConnected() async {
    final printer = selectedPrinter;
    if (printer == null) {
      errorMessage =
          '\u8acb\u5148\u6383\u63cf\u4e26\u9078\u64c7\u4e00\u53f0\u5370\u8868\u6a5f\uff0c\u518d\u9032\u884c\u5217\u5370\u3002';
      notifyListeners();
      return null;
    }

    if (printer.isConnected ?? false) {
      return printer;
    }

    await connectPrinter(printer);
    return selectedPrinter?.isConnected ?? false ? selectedPrinter : null;
  }

  PrinterTarget _toTarget(Printer printer) {
    return PrinterTarget(
      name: printer.name ?? 'Unnamed Printer',
      connectionType: printer.connectionTypeString,
      address: printer.address,
      vendorId: printer.vendorId,
      productId: printer.productId,
    );
  }

  String _printerLabel(Printer printer) {
    return printer.name?.trim().isNotEmpty == true
        ? printer.name!.trim()
        : (printer.address ?? 'Unnamed Printer');
  }

  bool _matchesRememberedPrinter(Printer printer, PrinterTarget target) {
    if (printer.connectionTypeString != target.connectionType) {
      return false;
    }

    final printerAddress = printer.address?.trim();
    final targetAddress = target.address?.trim();
    if (printerAddress != null &&
        printerAddress.isNotEmpty &&
        targetAddress != null &&
        targetAddress.isNotEmpty) {
      return printerAddress == targetAddress;
    }

    final printerVendor = printer.vendorId?.trim();
    final printerProduct = printer.productId?.trim();
    final targetVendor = target.vendorId?.trim();
    final targetProduct = target.productId?.trim();
    if (printerVendor != null &&
        printerProduct != null &&
        targetVendor != null &&
        targetProduct != null &&
        printerVendor.isNotEmpty &&
        printerProduct.isNotEmpty &&
        targetVendor.isNotEmpty &&
        targetProduct.isNotEmpty) {
      return printerVendor == targetVendor && printerProduct == targetProduct;
    }

    return (printer.name ?? '').trim() == target.name.trim();
  }

  bool _isSamePrinter(Printer left, Printer right) {
    final leftAddress = left.address?.trim();
    final rightAddress = right.address?.trim();
    if (left.connectionTypeString == right.connectionTypeString &&
        leftAddress != null &&
        leftAddress.isNotEmpty &&
        rightAddress != null &&
        rightAddress.isNotEmpty) {
      return leftAddress == rightAddress;
    }

    final leftVendor = left.vendorId?.trim();
    final leftProduct = left.productId?.trim();
    final rightVendor = right.vendorId?.trim();
    final rightProduct = right.productId?.trim();
    if (left.connectionTypeString == right.connectionTypeString &&
        leftVendor != null &&
        leftVendor.isNotEmpty &&
        leftProduct != null &&
        leftProduct.isNotEmpty &&
        rightVendor != null &&
        rightVendor.isNotEmpty &&
        rightProduct != null &&
        rightProduct.isNotEmpty) {
      return leftVendor == rightVendor && leftProduct == rightProduct;
    }

    return left.connectionTypeString == right.connectionTypeString &&
        (left.name ?? '').trim() == (right.name ?? '').trim();
  }

  Printer _mergePrinter(Printer printer, {required bool connected}) {
    return Printer(
      address: printer.address,
      name: printer.name,
      connectionType: printer.connectionType,
      isConnected: connected,
      vendorId: printer.vendorId,
      productId: printer.productId,
    );
  }

  @override
  void dispose() {
    _devicesSubscription?.cancel();
    _printerService.stopDiscovery();
    super.dispose();
  }
}
