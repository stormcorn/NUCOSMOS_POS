import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_thermal_printer/utils/printer.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/classic_bluetooth_device.dart';
import '../models/classic_bluetooth_status.dart';
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
  List<ClassicBluetoothDevice> classicBluetoothDevices = const [];
  Printer? selectedPrinter;
  ClassicBluetoothDevice? selectedClassicDevice;
  PrinterTarget? rememberedPrinter;

  bool restoring = true;
  bool scanning = false;
  bool connecting = false;
  bool printing = false;
  bool autoPrintReceipt = true;

  String statusMessage = '';
  String errorMessage = '';
  ClassicBluetoothStatus classicStatus = const ClassicBluetoothStatus(
    bluetoothEnabled: false,
    missingPermissions: <String>[],
    bondedDeviceCount: 0,
  );

  bool get hasConnectedPrinter =>
      (selectedPrinter?.isConnected ?? false) ||
      (selectedClassicDevice?.isConnected ?? false);

  bool get hasSelectedPrinter =>
      selectedPrinter != null || selectedClassicDevice != null;

  String get selectedPrinterSummary {
    if (selectedPrinter != null) {
      return selectedPrinter!.name?.trim().isNotEmpty == true
          ? selectedPrinter!.name!.trim()
          : (selectedPrinter!.address ?? 'Unnamed Printer');
    }
    if (selectedClassicDevice != null) {
      return selectedClassicDevice!.name;
    }
    return '\u5c1a\u672a\u9078\u64c7\u5370\u8868\u6a5f';
  }

  String get selectedPrinterStatusSummary {
    if (selectedPrinter != null) {
      return '\u9023\u7dda\u72c0\u614b\uff1a${(selectedPrinter!.isConnected ?? false) ? '\u5df2\u9023\u7dda' : '\u672a\u9023\u7dda'} / ${selectedPrinter!.connectionTypeString}';
    }
    if (selectedClassicDevice != null) {
      return '\u9023\u7dda\u72c0\u614b\uff1a${selectedClassicDevice!.isConnected ? '\u5df2\u9023\u7dda' : '\u672a\u9023\u7dda'} / CLASSIC';
    }
    return '\u652f\u63f4\u85cd\u7259 BLE / Classic \u8207 USB \u71b1\u611f\u5370\u8868\u6a5f';
  }

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
    await refreshClassicBluetoothStatus();

    restoring = false;
    notifyListeners();
  }

  Future<void> refreshClassicBluetoothStatus() async {
    try {
      classicStatus = await _printerService.getClassicBluetoothStatus();
      if (selectedClassicDevice != null) {
        final connectedAddress = classicStatus.connectedAddress;
        selectedClassicDevice = ClassicBluetoothDevice(
          name: selectedClassicDevice!.name,
          address: selectedClassicDevice!.address,
          bondState: selectedClassicDevice!.bondState,
          isConnected: connectedAddress == selectedClassicDevice!.address,
        );
      }
    } catch (error) {
      errorMessage =
          '\u8b80\u53d6 Classic \u85cd\u7259\u72c0\u614b\u5931\u6557\uff1a$error';
    }
    notifyListeners();
  }

  Future<void> requestClassicBluetoothPermissions() async {
    errorMessage = '';
    try {
      classicStatus =
          await _printerService.requestClassicBluetoothPermissions();
      if (!classicStatus.hasAllPermissions) {
        errorMessage =
            '\u85cd\u7259\u6383\u63cf\u6b0a\u9650\u4ecd\u672a\u5b8c\u6574\uff0c\u8acb\u5141\u8a31 APP \u4f7f\u7528\u9644\u8fd1\u88dd\u7f6e\u8207\u85cd\u7259\u6b0a\u9650\u3002';
      } else {
        statusMessage =
            '\u85cd\u7259\u6383\u63cf\u6b0a\u9650\u5df2\u66f4\u65b0\u3002';
      }
    } catch (error) {
      errorMessage =
          '\u7533\u8acb\u85cd\u7259\u6b0a\u9650\u5931\u6557\uff1a$error';
    }
    notifyListeners();
  }

  Future<void> openClassicBluetoothSettings() async {
    try {
      await _printerService.openClassicBluetoothSettings();
      statusMessage =
          '\u5df2\u6253\u958b Android \u85cd\u7259\u8a2d\u5b9a\u3002';
    } catch (error) {
      errorMessage =
          '\u7121\u6cd5\u6253\u958b Android \u85cd\u7259\u8a2d\u5b9a\uff1a$error';
    }
    notifyListeners();
  }

  Future<void> startScan() async {
    errorMessage = '';
    await refreshClassicBluetoothStatus();
    statusMessage =
        '\u6b63\u5728\u6383\u63cf\u9644\u8fd1\u7684 BLE / USB \u5370\u8868\u6a5f\uff0c\u4e26\u6aa2\u67e5 Classic \u85cd\u7259\u88dd\u7f6e...';
    scanning = true;
    notifyListeners();

    try {
      await _printerService.startDiscovery();
      try {
        final classicDevices =
            await _printerService.scanClassicBluetoothDevices();
        final connectedAddress = classicStatus.connectedAddress;
        classicBluetoothDevices = classicDevices
            .map(
              (device) => ClassicBluetoothDevice(
                name: device.name,
                address: device.address,
                bondState: device.bondState,
                isConnected: device.address == connectedAddress,
              ),
            )
            .toList(growable: false);
      } catch (error) {
        errorMessage =
            '\u50b3\u7d71\u85cd\u7259\u88dd\u7f6e\u5075\u6e2c\u5931\u6557\uff1a$error';
      }
      await refreshClassicBluetoothStatus();
      _updateStatusMessage();
    } catch (error) {
      scanning = false;
      errorMessage = '\u6383\u63cf\u5931\u6557\uff1a$error';
      notifyListeners();
    }
  }

  Future<void> stopScan() async {
    _printerService.stopDiscovery();
    scanning = false;
    if (statusMessage.contains('\u6b63\u5728\u6383\u63cf')) {
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
      if (selectedClassicDevice?.isConnected == true) {
        await _printerService.disconnectClassicBluetoothDevice();
      }

      await _printerService.connect(printer);
      selectedPrinter = _mergePrinter(printer, connected: true);
      selectedClassicDevice = null;
      rememberedPrinter = _toTarget(selectedPrinter!);
      statusMessage =
          '\u5df2\u9023\u7dda\u5230 ${_printerLabel(selectedPrinter!)}';
      await _persistPrinterSelection();
      await refreshClassicBluetoothStatus();
    } catch (error) {
      errorMessage = '\u9023\u7dda\u5931\u6557\uff1a$error';
    } finally {
      connecting = false;
      notifyListeners();
    }
  }

  Future<void> connectClassicBluetoothDevice(
    ClassicBluetoothDevice device,
  ) async {
    connecting = true;
    errorMessage = '';
    statusMessage =
        '\u6b63\u5728\u9023\u7dda Classic \u85cd\u7259\u88dd\u7f6e ${device.name}...';
    notifyListeners();

    try {
      if ((selectedPrinter?.isConnected ?? false) && selectedPrinter != null) {
        await _printerService.disconnect(selectedPrinter!);
      }
      await _printerService.connectClassicBluetoothDevice(device);
      await refreshClassicBluetoothStatus();
      classicBluetoothDevices = classicBluetoothDevices
          .map(
            (candidate) => ClassicBluetoothDevice(
              name: candidate.name,
              address: candidate.address,
              bondState: candidate.bondState,
              isConnected: candidate.address == device.address,
            ),
          )
          .toList(growable: false);
      selectedClassicDevice = classicBluetoothDevices.firstWhere(
        (candidate) => candidate.address == device.address,
        orElse: () => ClassicBluetoothDevice(
          name: device.name,
          address: device.address,
          bondState: device.bondState,
          isConnected: true,
        ),
      );
      selectedPrinter = null;
      statusMessage =
          '\u5df2\u9023\u7dda\u5230 Classic \u85cd\u7259\u88dd\u7f6e ${device.name}';
    } catch (error) {
      errorMessage =
          '\u9023\u7dda Classic \u85cd\u7259\u88dd\u7f6e\u5931\u6557\uff1a$error';
    } finally {
      connecting = false;
      notifyListeners();
    }
  }

  Future<void> disconnectSelectedPrinter() async {
    if (selectedClassicDevice != null) {
      connecting = true;
      notifyListeners();
      try {
        await _printerService.disconnectClassicBluetoothDevice();
        final current = selectedClassicDevice!;
        selectedClassicDevice = ClassicBluetoothDevice(
          name: current.name,
          address: current.address,
          bondState: current.bondState,
          isConnected: false,
        );
        await refreshClassicBluetoothStatus();
        statusMessage = '\u5df2\u4e2d\u65b7 ${current.name}';
      } catch (error) {
        errorMessage =
            '\u4e2d\u65b7 Classic \u85cd\u7259\u9023\u7dda\u5931\u6557\uff1a$error';
      } finally {
        connecting = false;
        notifyListeners();
      }
      return;
    }

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
    printing = true;
    errorMessage = '';
    statusMessage = '\u6b63\u5728\u5217\u5370\u6e2c\u8a66\u9801...';
    notifyListeners();

    try {
      if (selectedClassicDevice != null) {
        await _printerService.printClassicTestReceipt(selectedClassicDevice!);
      } else {
        final printer = await _ensureSelectedPrinterConnected();
        if (printer == null) {
          printing = false;
          return;
        }
        await _printerService.printTestReceipt(printer);
      }
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

    printing = true;
    errorMessage = '';
    statusMessage =
        '\u6b63\u5728\u5217\u5370\u8a02\u55ae ${receipt.orderNumber}...';
    notifyListeners();

    try {
      if (selectedClassicDevice != null) {
        await _printerService.printClassicOrderReceipt(
          device: selectedClassicDevice!,
          receipt: receipt,
          lines: lines,
          storeCode: storeCode,
          staffName: staffName,
        );
      } else {
        final printer = await _ensureSelectedPrinterConnected();
        if (printer == null) {
          printing = false;
          return;
        }
        await _printerService.printOrderReceipt(
          printer: printer,
          receipt: receipt,
          lines: lines,
          storeCode: storeCode,
          staffName: staffName,
        );
      }
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
    _updateStatusMessage();
    notifyListeners();
  }

  void _updateStatusMessage() {
    if (printers.isEmpty && classicBluetoothDevices.isEmpty) {
      statusMessage =
          '\u5c1a\u672a\u627e\u5230 BLE / USB \u5370\u8868\u6a5f\uff0c\u4e5f\u6c92\u6709\u5075\u6e2c\u5230 Classic \u85cd\u7259\u88dd\u7f6e\u3002';
      return;
    }

    if (printers.isEmpty && classicBluetoothDevices.isNotEmpty) {
      statusMessage =
          '\u5075\u6e2c\u5230 ${classicBluetoothDevices.length} \u500b Classic \u85cd\u7259\u88dd\u7f6e\uff0c\u53ef\u4ee5\u5148\u5617\u8a66\u914d\u5c0d\u4e26\u9023\u7dda\u6e2c\u8a66\u9801\u3002';
      return;
    }

    final classicSuffix = classicBluetoothDevices.isEmpty
        ? ''
        : '\uff0c\u53e6\u5916\u5075\u6e2c\u5230 ${classicBluetoothDevices.length} \u500b Classic \u85cd\u7259\u88dd\u7f6e';
    statusMessage =
        '\u5df2\u627e\u5230 ${printers.length} \u53f0 BLE / USB \u5370\u8868\u6a5f$classicSuffix\u3002';
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
    }

    if (selectedClassicDevice != null) {
      final current = selectedClassicDevice!;
      selectedClassicDevice =
          classicBluetoothDevices.cast<ClassicBluetoothDevice?>().firstWhere(
                (device) => device != null && device.address == current.address,
                orElse: () => current,
              );
    }

    if (selectedPrinter == null && rememberedPrinter != null) {
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
