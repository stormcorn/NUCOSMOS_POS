import '../models/api_envelope.dart';
import '../models/order_models.dart';
import 'api_client.dart';

class OrderService {
  OrderService(this._apiClient);

  final ApiClient _apiClient;

  void updateBaseUrl(String baseUrl) {
    _apiClient.baseUrl = baseUrl;
  }

  Future<OrderReceipt> createOrder({
    required String accessToken,
    required List<OrderCreateItem> items,
    String? note,
    double? discountAmount,
    String? discountNote,
  }) async {
    final json = await _apiClient.post(
      '/api/v1/orders',
      accessToken: accessToken,
      body: {
        'items': items.map((item) => item.toJson()).toList(),
        if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
        if (discountAmount != null && discountAmount > 0)
          'discountAmount': discountAmount,
        if (discountNote != null && discountNote.trim().isNotEmpty)
          'discountNote': discountNote.trim(),
      },
    );

    return ApiEnvelope<OrderReceipt>.fromJson(json, OrderReceipt.fromJson).data;
  }

  Future<OrderReceipt> addCashPayment({
    required String accessToken,
    required String orderId,
    required double amount,
    String? note,
  }) async {
    final json = await _apiClient.post(
      '/api/v1/orders/$orderId/payments',
      accessToken: accessToken,
      body: {
        'paymentMethod': 'CASH',
        'amount': amount,
        'amountReceived': amount,
        if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
      },
    );

    return ApiEnvelope<OrderReceipt>.fromJson(json, OrderReceipt.fromJson).data;
  }

  Future<OrderReceipt> addOtherPayment({
    required String accessToken,
    required String orderId,
    String? note,
  }) async {
    final json = await _apiClient.post(
      '/api/v1/orders/$orderId/payments',
      accessToken: accessToken,
      body: {
        'paymentMethod': 'OTHER',
        'amount': 0,
        'amountReceived': 0,
        if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
      },
    );

    return ApiEnvelope<OrderReceipt>.fromJson(json, OrderReceipt.fromJson).data;
  }
}
