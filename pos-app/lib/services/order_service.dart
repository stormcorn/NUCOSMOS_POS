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
    CheckoutDiscount? discount,
  }) async {
    final json = await _apiClient.post(
      '/api/v1/orders',
      accessToken: accessToken,
      body: {
        'items': items.map((item) => item.toJson()).toList(),
        if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
        if (discount != null) 'discountType': discount.type.apiValue,
        if (discount != null && discount.type.requiresValue)
          'discountValue': discount.value,
        if (discount?.note != null && discount!.note!.trim().isNotEmpty)
          'discountNote': discount.note!.trim(),
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

  Future<OrderListPage> listOrders({
    required String accessToken,
    int page = 0,
    int size = 50,
  }) async {
    final json = await _apiClient.get(
      '/api/v1/orders?page=$page&size=$size&sortBy=orderedAt&sortDirection=desc',
      accessToken: accessToken,
    );

    return ApiEnvelope<OrderListPage>.fromJson(json, OrderListPage.fromJson)
        .data;
  }

  Future<PosOrderDetail> getOrderDetail({
    required String accessToken,
    required String orderId,
  }) async {
    final json = await _apiClient.get(
      '/api/v1/orders/$orderId',
      accessToken: accessToken,
    );

    return ApiEnvelope<PosOrderDetail>.fromJson(json, PosOrderDetail.fromJson)
        .data;
  }

  Future<OrderReceipt> cancelUnpaidOrder({
    required String accessToken,
    required String orderId,
    String? reason,
  }) async {
    final json = await _apiClient.post(
      '/api/v1/orders/$orderId/cancel',
      accessToken: accessToken,
      body: {
        if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
      },
    );

    return ApiEnvelope<OrderReceipt>.fromJson(json, OrderReceipt.fromJson).data;
  }

  Future<OrderReceipt> refundCashOrder({
    required String accessToken,
    required PosOrderDetail order,
    String? reason,
  }) async {
    final payment = order.latestRefundableCashPayment;
    if (payment == null) {
      throw ApiException('找不到可退款的現金付款紀錄', 400);
    }

    final json = await _apiClient.post(
      '/api/v1/orders/${order.id}/refunds',
      accessToken: accessToken,
      body: {
        'paymentId': payment.id,
        'refundMethod': 'CASH',
        'amount': order.refundableAmount,
        if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
        'items': order.items
            .where((item) => item.quantity > 0)
            .map(
              (item) => {
                'orderItemId': item.id,
                'quantity': item.quantity,
                'inventoryDisposition': 'SELLABLE',
              },
            )
            .toList(growable: false),
      },
    );

    return ApiEnvelope<OrderReceipt>.fromJson(json, OrderReceipt.fromJson).data;
  }
}
