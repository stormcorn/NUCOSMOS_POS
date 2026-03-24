import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../config/app_config.dart';
import '../models/auth_models.dart';
import '../models/order_models.dart';
import '../models/product_summary.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../services/order_service.dart';
import '../services/product_service.dart';

class SessionController extends ChangeNotifier {
  SessionController({
    required AuthService authService,
    required ProductService productService,
    required OrderService orderService,
    required String defaultApiBaseUrl,
    required String defaultDeviceCode,
  })  : _authService = authService,
        _productService = productService,
        _orderService = orderService,
        _apiBaseUrl = defaultApiBaseUrl,
        _deviceCode = defaultDeviceCode;

  static const _tokenKey = 'pos.access_token';
  static const _deviceCodeKey = 'pos.device_code';
  static const _apiBaseUrlKey = 'pos.api_base_url';

  final AuthService _authService;
  final ProductService _productService;
  final OrderService _orderService;

  bool bootstrapping = true;
  bool loading = false;
  bool catalogLoading = false;
  bool checkoutLoading = false;
  String errorMessage = '';
  String checkoutMessage = '';

  String _apiBaseUrl;
  String get apiBaseUrl => _apiBaseUrl;

  String _deviceCode;
  String get deviceCode => _deviceCode;

  String? accessToken;
  CurrentSession? session;
  List<ProductSummary> products = const [];
  String? selectedCategoryCode;
  List<PosCartLine> cart = const [];
  OrderReceipt? lastCompletedOrder;

  bool get isLoggedIn => accessToken != null && session != null;

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

    final result = counts.values.toList(growable: false)
      ..sort((left, right) => left.name.compareTo(right.name));
    return result;
  }

  int get cartItemCount =>
      cart.fold<int>(0, (total, line) => total + line.quantity);

  double get cartSubtotal =>
      cart.fold<double>(0, (total, line) => total + line.lineTotal);

  Future<void> restoreSession() async {
    bootstrapping = true;
    notifyListeners();

    final prefs = await SharedPreferences.getInstance();
    final storedToken = prefs.getString(_tokenKey);
    _apiBaseUrl = prefs.getString(_apiBaseUrlKey) ?? _apiBaseUrl;
    _deviceCode = prefs.getString(_deviceCodeKey) ?? _deviceCode;
    _syncApiBaseUrl();

    if (storedToken == null || storedToken.isEmpty) {
      bootstrapping = false;
      notifyListeners();
      return;
    }

    try {
      accessToken = storedToken;
      session = await _authService.currentSession(storedToken);
      await loadProducts(showLoading: false);
    } catch (_) {
      await logout();
    } finally {
      bootstrapping = false;
      notifyListeners();
    }
  }

  Future<bool> login({
    required String storeCode,
    required String roleCode,
    required String pin,
    required String deviceCode,
  }) async {
    loading = true;
    errorMessage = '';
    checkoutMessage = '';
    notifyListeners();

    try {
      final response = await _authService.pinLogin(
        storeCode: storeCode,
        roleCode: roleCode,
        pin: pin,
        deviceCode: deviceCode,
      );

      accessToken = response.accessToken;
      session = await _authService.currentSession(response.accessToken);
      _deviceCode = deviceCode;

      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_tokenKey, response.accessToken);
      await prefs.setString(_deviceCodeKey, deviceCode);
      await prefs.setString(_apiBaseUrlKey, _apiBaseUrl);

      await loadProducts(showLoading: false);
      return true;
    } on ApiException catch (error) {
      errorMessage = error.message;
      return false;
    } on Exception {
      errorMessage =
          '無法連線到 POS 伺服器，請確認平板與 ${AppConfig.apiBaseUrl} 在同一網段。';
      return false;
    } catch (_) {
      errorMessage =
          '無法連線到 POS 伺服器，請確認平板與 $_apiBaseUrl 在同一網段。';
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
      products = await _productService.fetchProducts(accessToken!);
      errorMessage = '';

      final availableCodes = products.map((product) => product.categoryCode).toSet();
      if (selectedCategoryCode != null &&
          selectedCategoryCode!.isNotEmpty &&
          !availableCodes.contains(selectedCategoryCode)) {
        selectedCategoryCode = null;
      }
    } on ApiException catch (error) {
      errorMessage = error.message;
    } on Exception {
      errorMessage = '商品載入失敗，請確認可連到 $_apiBaseUrl。';
    } catch (_) {
      errorMessage = '商品載入失敗，請確認可連到 $_apiBaseUrl。';
    } finally {
      catalogLoading = false;
      notifyListeners();
    }
  }

  void selectCategory(String? categoryCode) {
    selectedCategoryCode =
        categoryCode == null || categoryCode.isEmpty ? null : categoryCode;
    notifyListeners();
  }

  void addProduct(ProductSummary product) {
    final nextCart = [...cart];
    final index = nextCart.indexWhere((line) => line.product.id == product.id);
    if (index >= 0) {
      nextCart[index] = nextCart[index].copyWith(
        quantity: nextCart[index].quantity + 1,
      );
    } else {
      nextCart.add(PosCartLine(product: product, quantity: 1));
    }

    cart = nextCart;
    checkoutMessage = '';
    notifyListeners();
  }

  void increaseQuantity(String productId) {
    final nextCart = [...cart];
    final index = nextCart.indexWhere((line) => line.product.id == productId);
    if (index < 0) {
      return;
    }

    nextCart[index] = nextCart[index].copyWith(
      quantity: nextCart[index].quantity + 1,
    );
    cart = nextCart;
    notifyListeners();
  }

  void decreaseQuantity(String productId) {
    final nextCart = [...cart];
    final index = nextCart.indexWhere((line) => line.product.id == productId);
    if (index < 0) {
      return;
    }

    final current = nextCart[index];
    if (current.quantity <= 1) {
      nextCart.removeAt(index);
    } else {
      nextCart[index] = current.copyWith(quantity: current.quantity - 1);
    }

    cart = nextCart;
    notifyListeners();
  }

  void removeProduct(String productId) {
    cart = cart
        .where((line) => line.product.id != productId)
        .toList(growable: false);
    notifyListeners();
  }

  void clearCart() {
    cart = const [];
    checkoutMessage = '';
    notifyListeners();
  }

  Future<OrderReceipt?> checkoutCash() async {
    if (accessToken == null || session == null) {
      errorMessage = '請先登入，再進行結帳。';
      notifyListeners();
      return null;
    }

    if (cart.isEmpty) {
      errorMessage = '目前訂單沒有商品。';
      notifyListeners();
      return null;
    }

    checkoutLoading = true;
    errorMessage = '';
    checkoutMessage = '';
    notifyListeners();

    try {
      final order = await _orderService.createOrder(
        accessToken: accessToken!,
        items: cart
            .map(
              (line) => OrderCreateItem(
                productId: line.product.id,
                quantity: line.quantity,
              ),
            )
            .toList(growable: false),
      );

      final paidOrder = await _orderService.addCashPayment(
        accessToken: accessToken!,
        orderId: order.id,
        amount: order.totalAmount,
        note: 'Flutter POS cash checkout',
      );

      lastCompletedOrder = paidOrder;
      cart = const [];
      checkoutMessage =
          '訂單 ${paidOrder.orderNumber} 已完成，收款 \$${paidOrder.paidAmount.toStringAsFixed(2)}。';
      await loadProducts(showLoading: false);
      return paidOrder;
    } on ApiException catch (error) {
      errorMessage = error.message;
      return null;
    } on Exception {
      errorMessage = '結帳失敗，請確認可連到 $_apiBaseUrl，且商品庫存足夠。';
      return null;
    } catch (_) {
      errorMessage = '結帳失敗，請確認可連到 $_apiBaseUrl，且商品庫存足夠。';
      return null;
    } finally {
      checkoutLoading = false;
      notifyListeners();
    }
  }

  Future<void> logout() async {
    accessToken = null;
    session = null;
    products = const [];
    cart = const [];
    errorMessage = '';
    checkoutMessage = '';
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
      await _authService.healthCheck();
      return true;
    } on ApiException catch (error) {
      errorMessage = error.message;
      return false;
    } on Exception {
      errorMessage = '無法連線到 $_apiBaseUrl。';
      return false;
    } catch (_) {
      errorMessage = '無法連線到 $_apiBaseUrl。';
      return false;
    } finally {
      notifyListeners();
    }
  }

  Future<void> updateApiBaseUrl(String value) async {
    _apiBaseUrl = value.trim().replaceAll(RegExp(r'/$'), '');
    _syncApiBaseUrl();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_apiBaseUrlKey, _apiBaseUrl);
    notifyListeners();
  }

  void updateDeviceCode(String value) {
    _deviceCode = value;
    notifyListeners();
  }

  void _syncApiBaseUrl() {
    final normalized = _apiBaseUrl.trim().replaceAll(RegExp(r'/$'), '');
    _apiBaseUrl = normalized.isEmpty ? AppConfig.apiBaseUrl : normalized;
    _authService.updateBaseUrl(_apiBaseUrl);
    _productService.updateBaseUrl(_apiBaseUrl);
    _orderService.updateBaseUrl(_apiBaseUrl);
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

class PosCartLine {
  const PosCartLine({
    required this.product,
    required this.quantity,
  });

  final ProductSummary product;
  final int quantity;

  double get lineTotal => product.price * quantity;

  PosCartLine copyWith({
    ProductSummary? product,
    int? quantity,
  }) {
    return PosCartLine(
      product: product ?? this.product,
      quantity: quantity ?? this.quantity,
    );
  }
}
