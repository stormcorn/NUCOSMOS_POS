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
    required this.totalAmount,
    required this.paidAmount,
    required this.changeAmount,
  });

  final String id;
  final String orderNumber;
  final String status;
  final String paymentStatus;
  final String paymentMethod;
  final int itemCount;
  final double subtotalAmount;
  final double totalAmount;
  final double paidAmount;
  final double changeAmount;

  factory OrderReceipt.fromJson(Map<String, dynamic> json) {
    return OrderReceipt(
      id: json['id']?.toString() ?? '',
      orderNumber: json['orderNumber'] as String? ?? '',
      status: json['status'] as String? ?? '',
      paymentStatus: json['paymentStatus'] as String? ?? '',
      paymentMethod: _resolvePaymentMethod(json),
      itemCount: (json['itemCount'] as num?)?.toInt() ?? 0,
      subtotalAmount: (json['subtotalAmount'] as num?)?.toDouble() ?? 0,
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
      paidAmount: (json['paidAmount'] as num?)?.toDouble() ?? 0,
      changeAmount: (json['changeAmount'] as num?)?.toDouble() ?? 0,
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
