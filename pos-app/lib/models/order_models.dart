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
