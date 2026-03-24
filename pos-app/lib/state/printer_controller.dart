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
    statusMessage = '正在掃描藍牙 / USB 印表機...';
    scanning = true;
    notifyListeners();

    try {
      await _printerService.startDiscovery();
    } catch (error) {
      scanning = false;
      errorMessage = '掃描失敗：$error';
      notifyListeners();
    }
  }

  Future<void> stopScan() async {
    _printerService.stopDiscovery();
    scanning = false;
    if (statusMessage == '正在掃描藍牙 / USB 印表機...') {
      statusMessage = '已停止掃描';
    }
    notifyListeners();
  }

  Future<void> connectPrinter(Printer printer) async {
    connecting = true;
    errorMessage = '';
    statusMessage = '正在連線到 ${_printerLabel(printer)}...';
    notifyListeners();

    try {
      if ((selectedPrinter?.isConnected ?? false) &&
          !_isSamePrinter(selectedPrinter!, printer)) {
        await _printerService.disconnect(selectedPrinter!);
      }

      await _printerService.connect(printer);
      selectedPrinter = _mergePrinter(printer, connected: true);
      rememberedPrinter = _toTarget(selectedPrinter!);
      statusMessage = '已連線到 ${_printerLabel(selectedPrinter!)}';
      await _persistPrinterSelection();
    } catch (error) {
      errorMessage = '連線失敗：$error';
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
      statusMessage = '已中斷 ${_printerLabel(printer)}';
    } catch (error) {
      errorMessage = '中斷連線失敗：$error';
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
    statusMessage = '正在列印測試單...';
    notifyListeners();

    try {
      await _printerService.printTestReceipt(printer);
      statusMessage = '測試列印完成';
    } catch (error) {
      errorMessage = '測試列印失敗：$error';
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
    statusMessage = '正在列印訂單 ${receipt.orderNumber}...';
    notifyListeners();

    try {
      await _printerService.printOrderReceipt(
        printer: printer,
        receipt: receipt,
        lines: lines,
        storeCode: storeCode,
        staffName: staffName,
      );
      statusMessage = '訂單 ${receipt.orderNumber} 列印完成';
    } catch (error) {
      errorMessage = '訂單列印失敗：$error';
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
    printers = devices
        .where((printer) {
          final name = (printer.name ?? '').trim();
          return name.isNotEmpty;
        })
        .toList(growable: false);

    scanning = false;
    _refreshSelectedPrinterFromDevices();
    if (printers.isEmpty) {
      statusMessage = '未找到可用的藍牙 / USB 印表機';
    } else {
      statusMessage = '找到 ${printers.length} 台印表機';
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
              printer != null && _matchesRememberedPrinter(printer, rememberedPrinter!),
          orElse: () => null,
        );
    if (match != null) {
      selectedPrinter = match;
      statusMessage = '找到已記住的印表機：${_printerLabel(match)}';
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
      errorMessage = '請先掃描並連線一台熱感出單機。';
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
