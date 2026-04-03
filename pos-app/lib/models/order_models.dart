enum CheckoutDiscountType {
  percentage,
  amount,
  complimentary;

  String get apiValue {
    switch (this) {
      case CheckoutDiscountType.percentage:
        return 'PERCENTAGE';
      case CheckoutDiscountType.amount:
        return 'AMOUNT';
      case CheckoutDiscountType.complimentary:
        return 'COMPLIMENTARY';
    }
  }

  String get label {
    switch (this) {
      case CheckoutDiscountType.percentage:
        return '折扣';
      case CheckoutDiscountType.amount:
        return '抵用';
      case CheckoutDiscountType.complimentary:
        return '招待';
    }
  }

  String get summaryLabel {
    switch (this) {
      case CheckoutDiscountType.percentage:
        return '折扣優惠';
      case CheckoutDiscountType.amount:
        return '抵用優惠';
      case CheckoutDiscountType.complimentary:
        return '招待';
    }
  }

  bool get requiresValue => this != CheckoutDiscountType.complimentary;

  static CheckoutDiscountType? fromApiValue(String? value) {
    switch ((value ?? '').trim().toUpperCase()) {
      case 'PERCENTAGE':
        return CheckoutDiscountType.percentage;
      case 'AMOUNT':
        return CheckoutDiscountType.amount;
      case 'COMPLIMENTARY':
        return CheckoutDiscountType.complimentary;
      default:
        return null;
    }
  }
}

class CheckoutDiscount {
  const CheckoutDiscount({
    required this.type,
    required this.value,
    this.note,
  });

  final CheckoutDiscountType type;
  final double value;
  final String? note;

  double amountForSubtotal(double subtotal) {
    if (subtotal <= 0) {
      return 0;
    }

    switch (type) {
      case CheckoutDiscountType.percentage:
        return double.parse((subtotal * (value / 100)).toStringAsFixed(2));
      case CheckoutDiscountType.amount:
        return value > subtotal ? subtotal : value;
      case CheckoutDiscountType.complimentary:
        return double.parse(subtotal.toStringAsFixed(2));
    }
  }

  String get detailText {
    switch (type) {
      case CheckoutDiscountType.percentage:
        return '${value.toStringAsFixed(value == value.roundToDouble() ? 0 : 2)}%';
      case CheckoutDiscountType.amount:
        return '\$${value.toStringAsFixed(2)}';
      case CheckoutDiscountType.complimentary:
        return '整筆招待';
    }
  }
}

class OrderCreateItem {
  const OrderCreateItem({
    required this.productId,
    required this.quantity,
    this.note,
    this.selectedOptionIds = const [],
  });

  final String productId;
  final int quantity;
  final String? note;
  final List<String> selectedOptionIds;

  Map<String, dynamic> toJson() {
    return {
      'productId': productId,
      'quantity': quantity,
      if (note != null && note!.trim().isNotEmpty) 'note': note,
      if (selectedOptionIds.isNotEmpty) 'selectedOptionIds': selectedOptionIds,
    };
  }
}

class OrderReceipt {
  const OrderReceipt({
    required this.id,
    required this.orderNumber,
    required this.status,
    required this.paymentStatus,
    required this.paymentMethod,
    required this.itemCount,
    required this.subtotalAmount,
    this.discountType,
    this.discountValue,
    required this.discountAmount,
    required this.totalAmount,
    required this.paidAmount,
    required this.changeAmount,
    this.discountNote,
    this.redeemCode,
    this.redeemUrl,
  });

  final String id;
  final String orderNumber;
  final String status;
  final String paymentStatus;
  final String paymentMethod;
  final int itemCount;
  final double subtotalAmount;
  final CheckoutDiscountType? discountType;
  final double? discountValue;
  final double discountAmount;
  final double totalAmount;
  final double paidAmount;
  final double changeAmount;
  final String? discountNote;
  final String? redeemCode;
  final String? redeemUrl;

  factory OrderReceipt.fromJson(Map<String, dynamic> json) {
    return OrderReceipt(
      id: json['id']?.toString() ?? '',
      orderNumber: json['orderNumber'] as String? ?? '',
      status: json['status'] as String? ?? '',
      paymentStatus: json['paymentStatus'] as String? ?? '',
      paymentMethod: _resolvePaymentMethod(json),
      itemCount: (json['itemCount'] as num?)?.toInt() ?? 0,
      subtotalAmount: (json['subtotalAmount'] as num?)?.toDouble() ?? 0,
      discountType: CheckoutDiscountType.fromApiValue(
        json['discountType'] as String?,
      ),
      discountValue: (json['discountValue'] as num?)?.toDouble(),
      discountAmount: (json['discountAmount'] as num?)?.toDouble() ?? 0,
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
      paidAmount: (json['paidAmount'] as num?)?.toDouble() ?? 0,
      changeAmount: (json['changeAmount'] as num?)?.toDouble() ?? 0,
      discountNote: json['discountNote'] as String?,
      redeemCode: json['redeemCode'] as String?,
      redeemUrl: json['redeemUrl'] as String?,
    );
  }

  static String _resolvePaymentMethod(Map<String, dynamic> json) {
    final payments = json['payments'];
    if (payments is List && payments.isNotEmpty) {
      final latest = payments.last;
      if (latest is Map<String, dynamic>) {
        return latest['paymentMethod']?.toString() ?? '';
      }
      if (latest is Map) {
        return latest['paymentMethod']?.toString() ?? '';
      }
    }
    return '';
  }
}

class OrderListPage {
  const OrderListPage({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
    required this.hasNext,
  });

  final List<PosOrderSummary> items;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool hasNext;

  factory OrderListPage.fromJson(Map<String, dynamic> json) {
    final rawItems = json['items'];
    final items = rawItems is List
        ? rawItems
            .whereType<Map>()
            .map((item) => PosOrderSummary.fromJson(item.cast<String, dynamic>()))
            .toList(growable: false)
        : const <PosOrderSummary>[];

    return OrderListPage(
      items: items,
      page: (json['page'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? items.length,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? items.length,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 1,
      hasNext: json['hasNext'] as bool? ?? false,
    );
  }
}

class PosOrderSummary {
  const PosOrderSummary({
    required this.id,
    required this.orderNumber,
    required this.testOrder,
    required this.status,
    required this.paymentStatus,
    required this.storeCode,
    required this.deviceCode,
    required this.createdByEmployeeCode,
    required this.itemCount,
    required this.totalAmount,
    required this.paidAmount,
    required this.refundedAmount,
    required this.orderedAt,
    required this.closedAt,
  });

  final String id;
  final String orderNumber;
  final bool testOrder;
  final String status;
  final String paymentStatus;
  final String storeCode;
  final String? deviceCode;
  final String createdByEmployeeCode;
  final int itemCount;
  final double totalAmount;
  final double paidAmount;
  final double refundedAmount;
  final DateTime? orderedAt;
  final DateTime? closedAt;

  bool get isCancelled => status == 'VOIDED' || paymentStatus == 'VOIDED';
  bool get isFullyRefunded =>
      status == 'REFUNDED' || paymentStatus == 'REFUNDED';

  factory PosOrderSummary.fromJson(Map<String, dynamic> json) {
    return PosOrderSummary(
      id: json['id']?.toString() ?? '',
      orderNumber: json['orderNumber'] as String? ?? '',
      testOrder: json['testOrder'] as bool? ?? false,
      status: json['status'] as String? ?? '',
      paymentStatus: json['paymentStatus'] as String? ?? '',
      storeCode: json['storeCode'] as String? ?? '',
      deviceCode: json['deviceCode'] as String?,
      createdByEmployeeCode: json['createdByEmployeeCode'] as String? ?? '',
      itemCount: (json['itemCount'] as num?)?.toInt() ?? 0,
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
      paidAmount: (json['paidAmount'] as num?)?.toDouble() ?? 0,
      refundedAmount: (json['refundedAmount'] as num?)?.toDouble() ?? 0,
      orderedAt: _parseDateTime(json['orderedAt']),
      closedAt: _parseDateTime(json['closedAt']),
    );
  }
}

class PosOrderDetail {
  const PosOrderDetail({
    required this.id,
    required this.orderNumber,
    required this.testOrder,
    required this.status,
    required this.paymentStatus,
    required this.storeCode,
    required this.deviceCode,
    required this.createdByEmployeeCode,
    required this.itemCount,
    required this.subtotalAmount,
    required this.discountType,
    required this.discountValue,
    required this.discountAmount,
    required this.totalAmount,
    required this.paidAmount,
    required this.changeAmount,
    required this.refundedAmount,
    required this.note,
    required this.discountNote,
    required this.orderedAt,
    required this.closedAt,
    required this.voidedAt,
    required this.voidNote,
    required this.redeemCode,
    required this.redeemUrl,
    required this.items,
    required this.payments,
  });

  final String id;
  final String orderNumber;
  final bool testOrder;
  final String status;
  final String paymentStatus;
  final String storeCode;
  final String? deviceCode;
  final String createdByEmployeeCode;
  final int itemCount;
  final double subtotalAmount;
  final String? discountType;
  final double? discountValue;
  final double discountAmount;
  final double totalAmount;
  final double paidAmount;
  final double changeAmount;
  final double refundedAmount;
  final String? note;
  final String? discountNote;
  final DateTime? orderedAt;
  final DateTime? closedAt;
  final DateTime? voidedAt;
  final String? voidNote;
  final String? redeemCode;
  final String? redeemUrl;
  final List<PosOrderItemDetail> items;
  final List<PosPaymentDetail> payments;

  double get refundableAmount {
    final remaining = paidAmount - refundedAmount;
    return remaining > 0 ? remaining : 0;
  }

  bool get canVoidUnpaid => paymentStatus == 'UNPAID' && status != 'VOIDED';
  bool get canCashRefund =>
      refundableAmount > 0 &&
      payments.any((payment) =>
          payment.paymentMethod == 'CASH' &&
          (payment.status == 'CAPTURED' || payment.status == 'REFUNDED'));

  bool get isComplimentary =>
      payments.any((payment) => payment.paymentMethod == 'OTHER');

  PosPaymentDetail? get latestRefundableCashPayment {
    for (final payment in payments.reversed) {
      if (payment.paymentMethod == 'CASH' &&
          (payment.status == 'CAPTURED' || payment.status == 'REFUNDED')) {
        return payment;
      }
    }
    return null;
  }

  factory PosOrderDetail.fromJson(Map<String, dynamic> json) {
    final rawItems = json['items'];
    final rawPayments = json['payments'];

    return PosOrderDetail(
      id: json['id']?.toString() ?? '',
      orderNumber: json['orderNumber'] as String? ?? '',
      testOrder: json['testOrder'] as bool? ?? false,
      status: json['status'] as String? ?? '',
      paymentStatus: json['paymentStatus'] as String? ?? '',
      storeCode: json['storeCode'] as String? ?? '',
      deviceCode: json['deviceCode'] as String?,
      createdByEmployeeCode: json['createdByEmployeeCode'] as String? ?? '',
      itemCount: (json['itemCount'] as num?)?.toInt() ?? 0,
      subtotalAmount: (json['subtotalAmount'] as num?)?.toDouble() ?? 0,
      discountType: json['discountType'] as String?,
      discountValue: (json['discountValue'] as num?)?.toDouble(),
      discountAmount: (json['discountAmount'] as num?)?.toDouble() ?? 0,
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
      paidAmount: (json['paidAmount'] as num?)?.toDouble() ?? 0,
      changeAmount: (json['changeAmount'] as num?)?.toDouble() ?? 0,
      refundedAmount: (json['refundedAmount'] as num?)?.toDouble() ?? 0,
      note: json['note'] as String?,
      discountNote: json['discountNote'] as String?,
      orderedAt: _parseDateTime(json['orderedAt']),
      closedAt: _parseDateTime(json['closedAt']),
      voidedAt: _parseDateTime(json['voidedAt']),
      voidNote: json['voidNote'] as String?,
      redeemCode: json['redeemCode'] as String?,
      redeemUrl: json['redeemUrl'] as String?,
      items: rawItems is List
          ? rawItems
              .whereType<Map>()
              .map((item) =>
                  PosOrderItemDetail.fromJson(item.cast<String, dynamic>()))
              .toList(growable: false)
          : const <PosOrderItemDetail>[],
      payments: rawPayments is List
          ? rawPayments
              .whereType<Map>()
              .map((item) =>
                  PosPaymentDetail.fromJson(item.cast<String, dynamic>()))
              .toList(growable: false)
          : const <PosPaymentDetail>[],
    );
  }
}

class PosOrderItemDetail {
  const PosOrderItemDetail({
    required this.id,
    required this.productName,
    required this.quantity,
    required this.lineTotalAmount,
  });

  final String id;
  final String productName;
  final int quantity;
  final double lineTotalAmount;

  factory PosOrderItemDetail.fromJson(Map<String, dynamic> json) {
    return PosOrderItemDetail(
      id: json['id']?.toString() ?? '',
      productName: json['productName'] as String? ?? '',
      quantity: (json['quantity'] as num?)?.toInt() ?? 0,
      lineTotalAmount: (json['lineTotalAmount'] as num?)?.toDouble() ?? 0,
    );
  }
}

class PosPaymentDetail {
  const PosPaymentDetail({
    required this.id,
    required this.paymentMethod,
    required this.status,
    required this.amount,
  });

  final String id;
  final String paymentMethod;
  final String status;
  final double amount;

  factory PosPaymentDetail.fromJson(Map<String, dynamic> json) {
    return PosPaymentDetail(
      id: json['id']?.toString() ?? '',
      paymentMethod: json['paymentMethod'] as String? ?? '',
      status: json['status'] as String? ?? '',
      amount: (json['amount'] as num?)?.toDouble() ?? 0,
    );
  }
}

DateTime? _parseDateTime(Object? value) {
  final raw = value?.toString();
  if (raw == null || raw.isEmpty) {
    return null;
  }
  return DateTime.tryParse(raw)?.toLocal();
}
