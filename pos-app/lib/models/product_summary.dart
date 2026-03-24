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
    required this.customizationGroups,
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
  final List<ProductCustomizationGroup> customizationGroups;

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
      customizationGroups: ((json['customizationGroups'] as List?) ?? const [])
          .map((item) => ProductCustomizationGroup.fromJson((item as Map).cast<String, dynamic>()))
          .toList(growable: false),
    );
  }
}

class ProductCustomizationGroup {
  const ProductCustomizationGroup({
    required this.id,
    required this.name,
    required this.selectionMode,
    required this.required,
    required this.minSelections,
    required this.maxSelections,
    required this.displayOrder,
    required this.options,
  });

  final String id;
  final String name;
  final String selectionMode;
  final bool required;
  final int minSelections;
  final int maxSelections;
  final int displayOrder;
  final List<ProductCustomizationOption> options;

  bool get isSingleSelect => selectionMode.toUpperCase() == 'SINGLE';

  factory ProductCustomizationGroup.fromJson(Map<String, dynamic> json) {
    return ProductCustomizationGroup(
      id: json['id']?.toString() ?? '',
      name: json['name'] as String? ?? '',
      selectionMode: json['selectionMode'] as String? ?? 'SINGLE',
      required: json['required'] as bool? ?? false,
      minSelections: (json['minSelections'] as num?)?.toInt() ?? 0,
      maxSelections: (json['maxSelections'] as num?)?.toInt() ?? 1,
      displayOrder: (json['displayOrder'] as num?)?.toInt() ?? 0,
      options: ((json['options'] as List?) ?? const [])
          .map((item) => ProductCustomizationOption.fromJson((item as Map).cast<String, dynamic>()))
          .toList(growable: false),
    );
  }
}

class ProductCustomizationOption {
  const ProductCustomizationOption({
    required this.id,
    required this.name,
    required this.priceDelta,
    required this.defaultSelected,
    required this.displayOrder,
  });

  final String id;
  final String name;
  final double priceDelta;
  final bool defaultSelected;
  final int displayOrder;

  factory ProductCustomizationOption.fromJson(Map<String, dynamic> json) {
    return ProductCustomizationOption(
      id: json['id']?.toString() ?? '',
      name: json['name'] as String? ?? '',
      priceDelta: (json['priceDelta'] as num?)?.toDouble() ?? 0,
      defaultSelected: json['defaultSelected'] as bool? ?? false,
      displayOrder: (json['displayOrder'] as num?)?.toInt() ?? 0,
    );
  }
}
