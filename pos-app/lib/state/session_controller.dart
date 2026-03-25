import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../config/app_config.dart';
import '../models/auth_models.dart';
import '../models/order_models.dart';
import '../models/product_summary.dart';
import '../models/quick_receive_models.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../services/device_identity_service.dart';
import '../services/order_service.dart';
import '../services/product_service.dart';
import '../services/quick_receive_service.dart';

class SessionController extends ChangeNotifier {
  SessionController({
    required AuthService authService,
    required DeviceIdentityService deviceIdentityService,
    required ProductService productService,
    required OrderService orderService,
    required QuickReceiveService quickReceiveService,
    required String defaultApiBaseUrl,
    required String defaultDeviceCode,
  })  : _authService = authService,
        _deviceIdentityService = deviceIdentityService,
        _productService = productService,
        _orderService = orderService,
        _quickReceiveService = quickReceiveService,
        _apiBaseUrl = defaultApiBaseUrl,
        _deviceCode = defaultDeviceCode;

  static const _tokenKey = 'pos.access_token';
  static const _deviceCodeKey = 'pos.device_code';
  static const _deviceNameKey = 'pos.device_name';
  static const _devicePlatformKey = 'pos.device_platform';
  static const _selectedStoreCodeKey = 'pos.selected_store_code';
  static const _apiBaseUrlKey = 'pos.api_base_url';
  static const _fallbackStoreCode = 'TW001';

  final AuthService _authService;
  final DeviceIdentityService _deviceIdentityService;
  final ProductService _productService;
  final OrderService _orderService;
  final QuickReceiveService _quickReceiveService;

  bool bootstrapping = true;
  bool loading = false;
  bool catalogLoading = false;
  bool checkoutLoading = false;
  bool quickReceiveLoading = false;
  bool quickReceiveSaving = false;

  String errorMessage = '';
  String checkoutMessage = '';
  String quickReceiveMessage = '';

  String _apiBaseUrl;
  String get apiBaseUrl => _apiBaseUrl;
  bool get canEditApiBaseUrl => !kReleaseMode;

  String _deviceCode;
  String get deviceCode => _deviceCode;

  String _deviceName = 'POS Tablet';
  String get deviceName => _deviceName;

  String _devicePlatform = 'ANDROID';
  String get devicePlatform => _devicePlatform;

  String _deviceSummary = 'POS Tablet';
  String get deviceSummary => _deviceSummary;

  String? accessToken;
  CurrentSession? session;
  List<StoreSummary> availableStores = const [];
  String? selectedStoreCode;
  List<ProductSummary> products = const [];
  String? selectedCategoryCode;
  List<PosCartLine> cart = const [];
  OrderReceipt? lastCompletedOrder;
  List<QuickReceiveItem> receiveMaterials = const [];
  List<QuickReceiveItem> receiveManufacturedItems = const [];
  List<QuickReceiveItem> receivePackagingItems = const [];

  bool get isLoggedIn => accessToken != null && session != null;

  bool get canUseQuickReceive {
    final activeRole = session?.activeRole.toUpperCase() ?? '';
    return activeRole == 'MANAGER' || activeRole == 'ADMIN';
  }

  List<ProductSummary> get availableProducts =>
      products.where((product) => product.available).toList(growable: false);

  List<ProductSummary> get filteredProducts {
    final selected = selectedCategoryCode;
    final source = availableProducts;
    if (selected == null || selected.isEmpty) {
      return source;
    }

    return source
        .where((product) => product.categoryCode == selected)
        .toList(growable: false);
  }

  List<ProductCategoryFilter> get categories {
    final counts = <String, ProductCategoryFilter>{};
    for (final product in availableProducts) {
      final current = counts[product.categoryCode];
      if (current == null) {
        counts[product.categoryCode] = ProductCategoryFilter(
          code: product.categoryCode,
          name: product.categoryName,
          count: 1,
        );
      } else {
        counts[product.categoryCode] =
            current.copyWith(count: current.count + 1);
      }
    }

    final result = counts.values.toList(growable: true)
      ..sort((left, right) => left.name.compareTo(right.name));
    return List.unmodifiable(result);
  }

  int get cartItemCount =>
      cart.fold<int>(0, (total, line) => total + line.quantity);

  double get cartSubtotal =>
      cart.fold<double>(0, (total, line) => total + line.lineTotal);

  List<QuickReceiveItem> itemsForReceiveType(QuickReceiveItemType type) {
    final source = switch (type) {
      QuickReceiveItemType.material => receiveMaterials,
      QuickReceiveItemType.manufactured => receiveManufacturedItems,
      QuickReceiveItemType.packaging => receivePackagingItems,
    };

    final result = source
        .where((item) =>
            item.active && item.sku.isNotEmpty && item.name.isNotEmpty)
        .toList(growable: true)
      ..sort((left, right) {
        if (left.lowStock != right.lowStock) {
          return left.lowStock ? -1 : 1;
        }
        return left.name.compareTo(right.name);
      });

    return List.unmodifiable(result);
  }

  Future<void> restoreSession() async {
    bootstrapping = true;
    notifyListeners();

    final prefs = await SharedPreferences.getInstance();
    final storedToken = prefs.getString(_tokenKey);
    final storedApiBaseUrl = prefs.getString(_apiBaseUrlKey);
    _apiBaseUrl = _resolveInitialApiBaseUrl(storedApiBaseUrl);
    _deviceCode = prefs.getString(_deviceCodeKey) ?? _deviceCode;
    _deviceName = prefs.getString(_deviceNameKey) ?? _deviceName;
    _devicePlatform = prefs.getString(_devicePlatformKey) ?? _devicePlatform;
    selectedStoreCode = prefs.getString(_selectedStoreCodeKey);
    _syncApiBaseUrl();
    await _hydrateLoginContext(prefs);

    if (storedApiBaseUrl != null && storedApiBaseUrl != _apiBaseUrl) {
      await prefs.setString(_apiBaseUrlKey, _apiBaseUrl);
    }

    if (storedToken == null || storedToken.isEmpty) {
      bootstrapping = false;
      notifyListeners();
      return;
    }

    try {
      accessToken = storedToken;
      session = await _runWithApiFallback(
        () => _authService.currentSession(storedToken),
      );
      await loadProducts(showLoading: false);
      if (canUseQuickReceive) {
        await loadQuickReceiveCatalog(showLoading: false);
      }
    } catch (_) {
      await logout();
    } finally {
      bootstrapping = false;
      notifyListeners();
    }
  }

  Future<bool> login({
    required String storeCode,
    required String pin,
  }) async {
    loading = true;
    errorMessage = '';
    checkoutMessage = '';
    quickReceiveMessage = '';
    notifyListeners();

    try {
      final response = await _runWithApiFallback(
        () => _authService.pinLogin(
          storeCode: storeCode,
          pin: pin,
          deviceCode: _deviceCode,
          deviceName: _deviceName,
          devicePlatform: _devicePlatform,
        ),
      );

      accessToken = response.accessToken;
      session = await _runWithApiFallback(
        () => _authService.currentSession(response.accessToken),
      );
      _deviceCode =
          response.deviceCode.isEmpty ? _deviceCode : response.deviceCode;
      selectedStoreCode = storeCode;

      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_tokenKey, response.accessToken);
      await prefs.setString(_deviceCodeKey, _deviceCode);
      await prefs.setString(_deviceNameKey, _deviceName);
      await prefs.setString(_devicePlatformKey, _devicePlatform);
      await prefs.setString(_selectedStoreCodeKey, storeCode);
      await prefs.setString(_apiBaseUrlKey, _apiBaseUrl);

      await loadProducts(showLoading: false);
      if (canUseQuickReceive) {
        await loadQuickReceiveCatalog(showLoading: false);
      } else {
        receiveMaterials = const [];
        receiveManufacturedItems = const [];
        receivePackagingItems = const [];
      }

      return true;
    } on ApiException catch (error) {
      errorMessage = error.message;
      return false;
    } on Exception {
      errorMessage = '無法連線到 POS 伺服器，請檢查 API：$_apiBaseUrl';
      return false;
    } catch (_) {
      errorMessage = '無法連線到 POS 伺服器，請檢查 API：$_apiBaseUrl';
      return false;
    } finally {
      loading = false;
      notifyListeners();
    }
  }

  Future<void> loadProducts({bool showLoading = true}) async {
    if (accessToken == null) {
      products = const [];
      notifyListeners();
      return;
    }

    if (showLoading) {
      catalogLoading = true;
      notifyListeners();
    }

    try {
      products = await _runWithApiFallback(
        () => _productService.fetchProducts(accessToken!),
      );
      errorMessage = '';

      final availableCodes =
          products.map((product) => product.categoryCode).toSet();
      if (selectedCategoryCode != null &&
          selectedCategoryCode!.isNotEmpty &&
          !availableCodes.contains(selectedCategoryCode)) {
        selectedCategoryCode = null;
      }
    } on ApiException catch (error) {
      errorMessage = error.message;
    } on Exception {
      errorMessage = '無法取得商品資料，請檢查 API：$_apiBaseUrl';
    } catch (_) {
      errorMessage = '無法取得商品資料，請檢查 API：$_apiBaseUrl';
    } finally {
      catalogLoading = false;
      notifyListeners();
    }
  }

  Future<void> loadQuickReceiveCatalog({bool showLoading = true}) async {
    if (accessToken == null || !canUseQuickReceive) {
      receiveMaterials = const [];
      receiveManufacturedItems = const [];
      receivePackagingItems = const [];
      quickReceiveMessage = '';
      notifyListeners();
      return;
    }

    if (showLoading) {
      quickReceiveLoading = true;
      notifyListeners();
    }

    try {
      final results = await _runWithApiFallback(
        () => Future.wait<List<QuickReceiveItem>>([
          _quickReceiveService.fetchMaterials(accessToken!),
          _quickReceiveService.fetchManufacturedItems(accessToken!),
          _quickReceiveService.fetchPackagingItems(accessToken!),
        ]),
      );
      receiveMaterials = results[0];
      receiveManufacturedItems = results[1];
      receivePackagingItems = results[2];
      errorMessage = '';
    } on ApiException catch (error) {
      errorMessage = error.message;
    } on Exception {
      errorMessage = '無法取得收貨品項，請檢查 API：$_apiBaseUrl';
    } catch (_) {
      errorMessage = '無法取得收貨品項，請檢查 API：$_apiBaseUrl';
    } finally {
      quickReceiveLoading = false;
      notifyListeners();
    }
  }

  void selectCategory(String? categoryCode) {
    selectedCategoryCode =
        categoryCode == null || categoryCode.isEmpty ? null : categoryCode;
    notifyListeners();
  }

  void addProduct(
    ProductSummary product, {
    List<PosCartSelection> selectedOptions = const [],
  }) {
    final normalizedSelections = _normalizeSelections(selectedOptions);
    final nextCart = [...cart];
    final cartKey = PosCartLine.cartKeyFor(product.id, normalizedSelections);
    final index = nextCart.indexWhere((line) => line.key == cartKey);

    if (index >= 0) {
      nextCart[index] = nextCart[index].copyWith(
        quantity: nextCart[index].quantity + 1,
      );
    } else {
      nextCart.add(
        PosCartLine(
          product: product,
          quantity: 1,
          selectedOptions: normalizedSelections,
        ),
      );
    }

    cart = List.unmodifiable(nextCart);
    checkoutMessage = '';
    notifyListeners();
  }

  void increaseQuantity(String cartKey) {
    final nextCart = [...cart];
    final index = nextCart.indexWhere((line) => line.key == cartKey);
    if (index < 0) {
      return;
    }

    nextCart[index] = nextCart[index].copyWith(
      quantity: nextCart[index].quantity + 1,
    );
    cart = List.unmodifiable(nextCart);
    notifyListeners();
  }

  void decreaseQuantity(String cartKey) {
    final nextCart = [...cart];
    final index = nextCart.indexWhere((line) => line.key == cartKey);
    if (index < 0) {
      return;
    }

    final current = nextCart[index];
    if (current.quantity <= 1) {
      nextCart.removeAt(index);
    } else {
      nextCart[index] = current.copyWith(quantity: current.quantity - 1);
    }

    cart = List.unmodifiable(nextCart);
    notifyListeners();
  }

  void removeProduct(String cartKey) {
    cart = List.unmodifiable(
      cart.where((line) => line.key != cartKey).toList(growable: false),
    );
    notifyListeners();
  }

  void clearCart() {
    cart = const [];
    checkoutMessage = '';
    notifyListeners();
  }

  Future<OrderReceipt?> checkoutCash() async {
    if (accessToken == null || session == null) {
      errorMessage = '請先登入後再結帳。';
      notifyListeners();
      return null;
    }

    if (cart.isEmpty) {
      errorMessage = '購物車是空的，請先加入商品。';
      notifyListeners();
      return null;
    }

    checkoutLoading = true;
    errorMessage = '';
    checkoutMessage = '';
    notifyListeners();

    try {
      final order = await _runWithApiFallback(
        () => _orderService.createOrder(
          accessToken: accessToken!,
          items: cart
              .map(
                (line) => OrderCreateItem(
                  productId: line.product.id,
                  quantity: line.quantity,
                  selectedOptionIds: line.selectedOptions
                      .map((selection) => selection.optionId)
                      .toList(growable: false),
                ),
              )
              .toList(growable: false),
        ),
      );

      final paidOrder = await _runWithApiFallback(
        () => _orderService.addCashPayment(
          accessToken: accessToken!,
          orderId: order.id,
          amount: order.totalAmount,
          note: 'Flutter POS cash checkout',
        ),
      );

      lastCompletedOrder = paidOrder;
      cart = const [];
      checkoutMessage =
          '訂單 ${paidOrder.orderNumber} 已完成，現金收款 ${paidOrder.paidAmount.toStringAsFixed(2)} 元';
      await loadProducts(showLoading: false);
      return paidOrder;
    } on ApiException catch (error) {
      errorMessage = error.message;
      return null;
    } on Exception {
      errorMessage = '結帳失敗，請確認 API：$_apiBaseUrl';
      return null;
    } catch (_) {
      errorMessage = '結帳失敗，請確認 API：$_apiBaseUrl';
      return null;
    } finally {
      checkoutLoading = false;
      notifyListeners();
    }
  }

  Future<QuickReceiveResult?> submitQuickReceive({
    required QuickReceiveItem item,
    required int purchaseQuantity,
    double? purchaseUnitCost,
    String? note,
  }) async {
    if (accessToken == null || session == null) {
      errorMessage = '請先登入後再進行收貨。';
      notifyListeners();
      return null;
    }

    quickReceiveSaving = true;
    errorMessage = '';
    quickReceiveMessage = '';
    notifyListeners();

    try {
      final result = await _runWithApiFallback(
        () => _quickReceiveService.submitQuickReceive(
          accessToken: accessToken!,
          item: item,
          purchaseQuantity: purchaseQuantity,
          purchaseUnitCost: purchaseUnitCost,
          note: note,
        ),
      );
      quickReceiveMessage =
          '${item.type.label} ${result.itemName} 已收貨 $purchaseQuantity ${item.purchaseUnit}'
          '（入庫 ${result.receivedStockQuantity} ${item.unit}）';
      await loadQuickReceiveCatalog(showLoading: false);
      return result;
    } on ApiException catch (error) {
      errorMessage = error.message;
      return null;
    } on Exception {
      errorMessage = '快速收貨失敗，請檢查 API：$_apiBaseUrl';
      return null;
    } catch (_) {
      errorMessage = '快速收貨失敗，請檢查 API：$_apiBaseUrl';
      return null;
    } finally {
      quickReceiveSaving = false;
      notifyListeners();
    }
  }

  Future<QuickReceiveItem?> createQuickReceiveItem({
    required QuickReceiveItemType type,
    required String sku,
    required String name,
    required String unit,
    required String purchaseUnit,
    required int purchaseToStockRatio,
    required int reorderLevel,
    String? description,
    double? latestUnitCost,
  }) async {
    if (accessToken == null || session == null) {
      errorMessage = '請先登入後再新增收貨品項。';
      notifyListeners();
      return null;
    }

    quickReceiveSaving = true;
    errorMessage = '';
    quickReceiveMessage = '';
    notifyListeners();

    try {
      final createdItem = await _runWithApiFallback(
        () => _quickReceiveService.createItem(
          accessToken: accessToken!,
          type: type,
          sku: sku,
          name: name,
          unit: unit,
          purchaseUnit: purchaseUnit,
          purchaseToStockRatio: purchaseToStockRatio,
          reorderLevel: reorderLevel,
          description: description,
          latestUnitCost: latestUnitCost,
        ),
      );
      quickReceiveMessage = '${type.label} ${createdItem.name} 已建立，可直接收貨。';
      await loadQuickReceiveCatalog(showLoading: false);
      return createdItem;
    } on ApiException catch (error) {
      errorMessage = error.message;
      return null;
    } on Exception {
      errorMessage = '新增收貨品項失敗，請確認 API：$_apiBaseUrl';
      return null;
    } catch (_) {
      errorMessage = '新增收貨品項失敗，請確認 API：$_apiBaseUrl';
      return null;
    } finally {
      quickReceiveSaving = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    accessToken = null;
    session = null;
    products = const [];
    cart = const [];
    receiveMaterials = const [];
    receiveManufacturedItems = const [];
    receivePackagingItems = const [];
    errorMessage = '';
    checkoutMessage = '';
    quickReceiveMessage = '';
    lastCompletedOrder = null;
    selectedCategoryCode = null;

    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);

    notifyListeners();
  }

  Future<bool> testConnection() async {
    errorMessage = '';
    notifyListeners();

    try {
      await _runWithApiFallback(_authService.healthCheck);
      return true;
    } on ApiException catch (error) {
      errorMessage = error.message;
      return false;
    } on Exception {
      errorMessage = '目前無法連線到 $_apiBaseUrl';
      return false;
    } catch (_) {
      errorMessage = '目前無法連線到 $_apiBaseUrl';
      return false;
    } finally {
      notifyListeners();
    }
  }

  Future<void> updateApiBaseUrl(String value) async {
    if (!canEditApiBaseUrl) {
      _apiBaseUrl = AppConfig.apiBaseUrl;
      _syncApiBaseUrl();
      notifyListeners();
      return;
    }

    _apiBaseUrl = _sanitizeApiBaseUrl(value);
    _syncApiBaseUrl();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_apiBaseUrlKey, _apiBaseUrl);
    notifyListeners();
  }

  Future<void> updateSelectedStoreCode(String? value) async {
    selectedStoreCode = value;
    final prefs = await SharedPreferences.getInstance();
    if (value == null || value.isEmpty) {
      await prefs.remove(_selectedStoreCodeKey);
    } else {
      await prefs.setString(_selectedStoreCodeKey, value);
    }
    notifyListeners();
  }

  void _syncApiBaseUrl() {
    final normalized = _sanitizeApiBaseUrl(_apiBaseUrl);
    _apiBaseUrl = normalized.isEmpty ? AppConfig.apiBaseUrl : normalized;
    _authService.updateBaseUrl(_apiBaseUrl);
    _productService.updateBaseUrl(_apiBaseUrl);
    _orderService.updateBaseUrl(_apiBaseUrl);
    _quickReceiveService.updateBaseUrl(_apiBaseUrl);
  }

  Future<void> loadAvailableStores({bool notify = true}) async {
    try {
      final stores = await _runWithApiFallback(_authService.fetchStores);
      availableStores = stores.isEmpty
          ? const [
              StoreSummary(code: _fallbackStoreCode, name: _fallbackStoreCode)
            ]
          : stores;
      _ensureSelectedStoreCode();
      errorMessage = '';
    } on ApiException catch (error) {
      availableStores = _fallbackStores();
      _ensureSelectedStoreCode();
      errorMessage = error.message;
    } on Exception {
      availableStores = _fallbackStores();
      _ensureSelectedStoreCode();
    } catch (_) {
      availableStores = _fallbackStores();
      _ensureSelectedStoreCode();
    } finally {
      if (notify) {
        notifyListeners();
      }
    }
  }

  Future<void> _hydrateLoginContext(SharedPreferences prefs) async {
    await _loadDeviceIdentity(prefs);
    await loadAvailableStores(notify: false);
  }

  Future<void> _loadDeviceIdentity(SharedPreferences prefs) async {
    final identity = await _deviceIdentityService.resolveIdentity();
    _deviceCode = identity.deviceCode;
    _deviceName = identity.deviceName;
    _devicePlatform = identity.devicePlatform;
    _deviceSummary = identity.deviceSummary;
    await prefs.setString(_deviceCodeKey, _deviceCode);
    await prefs.setString(_deviceNameKey, _deviceName);
    await prefs.setString(_devicePlatformKey, _devicePlatform);
  }

  void _ensureSelectedStoreCode() {
    final codes = availableStores.map((store) => store.code).toSet();
    if (selectedStoreCode != null &&
        selectedStoreCode!.isNotEmpty &&
        codes.contains(selectedStoreCode)) {
      return;
    }
    selectedStoreCode = availableStores.isEmpty
        ? _fallbackStoreCode
        : availableStores.first.code;
  }

  List<StoreSummary> _fallbackStores() {
    final code = (selectedStoreCode ?? _fallbackStoreCode).trim();
    final normalized = code.isEmpty ? _fallbackStoreCode : code;
    return [
      StoreSummary(
        code: normalized,
        name: normalized == _fallbackStoreCode
            ? 'NUCOSMOS Demo Store'
            : normalized,
      ),
    ];
  }

  Future<T> _runWithApiFallback<T>(Future<T> Function() action) async {
    try {
      return await action();
    } on ApiException {
      rethrow;
    } on Exception {
      final fallbackUrl = _alternateApiBaseUrl(_apiBaseUrl);
      if (fallbackUrl == null) {
        rethrow;
      }

      final originalUrl = _apiBaseUrl;
      await _setApiBaseUrlSilently(fallbackUrl);

      try {
        return await action();
      } catch (_) {
        await _setApiBaseUrlSilently(originalUrl);
        rethrow;
      }
    }
  }

  Future<void> _setApiBaseUrlSilently(String value) async {
    _apiBaseUrl = _sanitizeApiBaseUrl(value);
    _syncApiBaseUrl();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_apiBaseUrlKey, _apiBaseUrl);
  }

  String _sanitizeApiBaseUrl(String value) {
    var normalized = value.trim();
    if (normalized.isEmpty) {
      normalized = AppConfig.apiBaseUrl;
    }

    if (!normalized.contains('://')) {
      const localOnlyHosts = {
        'localhost',
        '127.0.0.1',
        '10.0.2.2',
        '10.0.3.2',
      };
      normalized = localOnlyHosts.contains(normalized.toLowerCase())
          ? 'http://$normalized'
          : 'https://$normalized';
    }

    return normalized.replaceAll(RegExp(r'/$'), '');
  }

  String _resolveInitialApiBaseUrl(String? storedApiBaseUrl) {
    if (!canEditApiBaseUrl) {
      return AppConfig.apiBaseUrl;
    }

    final normalizedStored = _normalizeApiBaseUrl(storedApiBaseUrl);
    final normalizedDefault =
        _normalizeApiBaseUrl(AppConfig.apiBaseUrl) ?? AppConfig.apiBaseUrl;

    if (normalizedStored == null || normalizedStored.isEmpty) {
      return normalizedDefault;
    }

    if (_shouldReplaceLegacyLocalUrl(
      storedApiBaseUrl: normalizedStored,
      defaultApiBaseUrl: normalizedDefault,
    )) {
      return normalizedDefault;
    }

    return normalizedStored;
  }

  String? _normalizeApiBaseUrl(String? value) {
    final normalized = (value ?? '').trim().replaceAll(RegExp(r'/$'), '');
    if (normalized.isEmpty) {
      return null;
    }
    return normalized;
  }

  bool _shouldReplaceLegacyLocalUrl({
    required String storedApiBaseUrl,
    required String defaultApiBaseUrl,
  }) {
    final defaultUri = Uri.tryParse(defaultApiBaseUrl);
    final storedUri = Uri.tryParse(storedApiBaseUrl);
    if (defaultUri == null || storedUri == null) {
      return false;
    }

    final defaultHost = defaultUri.host.toLowerCase();
    final storedHost = storedUri.host.toLowerCase();
    const legacyHosts = {
      'localhost',
      '127.0.0.1',
      '10.0.2.2',
      '10.0.3.2',
    };
    final storedPath = storedUri.path.replaceAll(RegExp(r'/$'), '');

    return defaultHost == 'nucosmos.io' &&
        (legacyHosts.contains(storedHost) ||
            (storedHost == 'nucosmos.io' && storedPath == '/api'));
  }

  String? _alternateApiBaseUrl(String value) {
    if (!canEditApiBaseUrl) {
      return null;
    }

    final current = Uri.tryParse(_sanitizeApiBaseUrl(value));
    if (current == null) {
      return null;
    }

    final host = current.host.toLowerCase();
    const localOnlyHosts = {
      'localhost',
      '127.0.0.1',
      '10.0.2.2',
      '10.0.3.2',
    };

    if (localOnlyHosts.contains(host)) {
      return null;
    }

    if (current.scheme == 'http') {
      return current.replace(scheme: 'https').toString();
    }

    if (current.scheme == 'https') {
      return current.replace(scheme: 'http').toString();
    }

    return null;
  }

  List<PosCartSelection> _normalizeSelections(
      List<PosCartSelection> selections) {
    final nextSelections = [...selections]
      ..sort((left, right) => left.optionId.compareTo(right.optionId));
    return List.unmodifiable(nextSelections);
  }
}

class ProductCategoryFilter {
  const ProductCategoryFilter({
    required this.code,
    required this.name,
    required this.count,
  });

  final String code;
  final String name;
  final int count;

  ProductCategoryFilter copyWith({
    String? code,
    String? name,
    int? count,
  }) {
    return ProductCategoryFilter(
      code: code ?? this.code,
      name: name ?? this.name,
      count: count ?? this.count,
    );
  }
}

class PosCartSelection {
  const PosCartSelection({
    required this.groupId,
    required this.groupName,
    required this.optionId,
    required this.optionName,
    required this.priceDelta,
  });

  final String groupId;
  final String groupName;
  final String optionId;
  final String optionName;
  final double priceDelta;
}

class PosCartLine {
  const PosCartLine({
    required this.product,
    required this.quantity,
    this.selectedOptions = const [],
  });

  final ProductSummary product;
  final int quantity;
  final List<PosCartSelection> selectedOptions;

  double get unitPrice =>
      product.price +
      selectedOptions.fold<double>(
        0,
        (total, selection) => total + selection.priceDelta,
      );

  double get lineTotal => unitPrice * quantity;

  String get key => cartKeyFor(product.id, selectedOptions);

  static String cartKeyFor(
    String productId,
    List<PosCartSelection> selectedOptions,
  ) {
    final optionIds =
        selectedOptions.map((selection) => selection.optionId).toList()..sort();
    return '$productId::${optionIds.join(",")}';
  }

  PosCartLine copyWith({
    ProductSummary? product,
    int? quantity,
    List<PosCartSelection>? selectedOptions,
  }) {
    return PosCartLine(
      product: product ?? this.product,
      quantity: quantity ?? this.quantity,
      selectedOptions: selectedOptions ?? this.selectedOptions,
    );
  }
}
