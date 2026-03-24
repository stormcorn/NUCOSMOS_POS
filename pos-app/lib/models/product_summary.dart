class ProductSummary {
  const ProductSummary({
    required this.id,
    required this.sku,
    required this.name,
    required this.description,
    required this.imageUrl,
    required this.categoryCode,
    required this.categoryName,
    required this.originalPrice,
    required this.price,
    required this.campaignEnabled,
    required this.campaignActive,
    required this.campaignLabel,
    required this.campaignPrice,
    required this.campaignStartsAt,
    required this.campaignEndsAt,
    required this.available,
  });

  final String id;
  final String sku;
  final String name;
  final String? description;
  final String? imageUrl;
  final String categoryCode;
  final String categoryName;
  final double originalPrice;
  final double price;
  final bool campaignEnabled;
  final bool campaignActive;
  final String? campaignLabel;
  final double? campaignPrice;
  final DateTime? campaignStartsAt;
  final DateTime? campaignEndsAt;
  final bool available;

  factory ProductSummary.fromJson(Map<String, dynamic> json) {
    return ProductSummary(
      id: json['id']?.toString() ?? '',
      sku: json['sku'] as String? ?? '',
      name: json['name'] as String? ?? '',
      description: json['description'] as String?,
      imageUrl: json['imageUrl'] as String?,
      categoryCode: json['categoryCode'] as String? ?? '',
      categoryName: json['categoryName'] as String? ?? '',
      originalPrice: (json['originalPrice'] as num?)?.toDouble() ?? 0,
      price: (json['price'] as num?)?.toDouble() ?? 0,
      campaignEnabled: json['campaignEnabled'] as bool? ?? false,
      campaignActive: json['campaignActive'] as bool? ?? false,
      campaignLabel: json['campaignLabel'] as String?,
      campaignPrice: (json['campaignPrice'] as num?)?.toDouble(),
      campaignStartsAt: json['campaignStartsAt'] == null
          ? null
          : DateTime.tryParse(json['campaignStartsAt'] as String),
      campaignEndsAt: json['campaignEndsAt'] == null
          ? null
          : DateTime.tryParse(json['campaignEndsAt'] as String),
      available: json['available'] as bool? ?? true,
    );
  }
}
