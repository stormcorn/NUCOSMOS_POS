enum QuickReceiveItemType {
  material,
  packaging;

  String get label => this == QuickReceiveItemType.material ? '原料' : '包裝';

  String get apiPath =>
      this == QuickReceiveItemType.material
          ? '/api/v1/admin/materials'
          : '/api/v1/admin/packaging-items';
}

class QuickReceiveItem {
  const QuickReceiveItem({
    required this.id,
    required this.type,
    required this.sku,
    required this.name,
    required this.unit,
    required this.purchaseUnit,
    required this.purchaseToStockRatio,
    required this.quantityOnHand,
    required this.lowStock,
    required this.active,
    this.imageUrl,
    this.description,
    this.latestUnitCost,
    this.latestPurchaseUnitCost,
    this.specification,
  });

  final String id;
  final QuickReceiveItemType type;
  final String sku;
  final String name;
  final String unit;
  final String purchaseUnit;
  final int purchaseToStockRatio;
  final int quantityOnHand;
  final bool lowStock;
  final bool active;
  final String? imageUrl;
  final String? description;
  final double? latestUnitCost;
  final double? latestPurchaseUnitCost;
  final String? specification;

  String get subtitle =>
      type == QuickReceiveItemType.packaging && (specification?.trim().isNotEmpty ?? false)
          ? specification!.trim()
          : (description?.trim().isNotEmpty ?? false)
          ? description!.trim()
          : '$purchaseUnit x $purchaseToStockRatio $unit';

  factory QuickReceiveItem.fromMaterialJson(Map<String, dynamic> json) {
    return QuickReceiveItem(
      id: json['id']?.toString() ?? '',
      type: QuickReceiveItemType.material,
      sku: json['sku'] as String? ?? '',
      name: json['name'] as String? ?? '',
      unit: json['unit'] as String? ?? '',
      purchaseUnit: json['purchaseUnit'] as String? ?? '',
      purchaseToStockRatio: (json['purchaseToStockRatio'] as num?)?.toInt() ?? 1,
      quantityOnHand: (json['quantityOnHand'] as num?)?.toInt() ?? 0,
      lowStock: json['lowStock'] as bool? ?? false,
      active: json['active'] as bool? ?? true,
      imageUrl: json['imageUrl'] as String?,
      description: json['description'] as String?,
      latestUnitCost: (json['latestUnitCost'] as num?)?.toDouble(),
      latestPurchaseUnitCost: (json['latestPurchaseUnitCost'] as num?)?.toDouble(),
    );
  }

  factory QuickReceiveItem.fromPackagingJson(Map<String, dynamic> json) {
    return QuickReceiveItem(
      id: json['id']?.toString() ?? '',
      type: QuickReceiveItemType.packaging,
      sku: json['sku'] as String? ?? '',
      name: json['name'] as String? ?? '',
      unit: json['unit'] as String? ?? '',
      purchaseUnit: json['purchaseUnit'] as String? ?? '',
      purchaseToStockRatio: (json['purchaseToStockRatio'] as num?)?.toInt() ?? 1,
      quantityOnHand: (json['quantityOnHand'] as num?)?.toInt() ?? 0,
      lowStock: json['lowStock'] as bool? ?? false,
      active: json['active'] as bool? ?? true,
      imageUrl: json['imageUrl'] as String?,
      description: json['description'] as String?,
      latestUnitCost: (json['latestUnitCost'] as num?)?.toDouble(),
      latestPurchaseUnitCost: (json['latestPurchaseUnitCost'] as num?)?.toDouble(),
      specification: json['specification'] as String?,
    );
  }
}

class QuickReceiveResult {
  const QuickReceiveResult({
    required this.itemName,
    required this.itemType,
    required this.quantityAfter,
    required this.receivedStockQuantity,
  });

  final String itemName;
  final QuickReceiveItemType itemType;
  final int quantityAfter;
  final int receivedStockQuantity;

  factory QuickReceiveResult.fromMaterialJson(
    Map<String, dynamic> json, {
    required int receivedStockQuantity,
  }) {
    return QuickReceiveResult(
      itemName: json['materialName'] as String? ?? '',
      itemType: QuickReceiveItemType.material,
      quantityAfter: (json['quantityAfter'] as num?)?.toInt() ?? 0,
      receivedStockQuantity: receivedStockQuantity,
    );
  }

  factory QuickReceiveResult.fromPackagingJson(
    Map<String, dynamic> json, {
    required int receivedStockQuantity,
  }) {
    return QuickReceiveResult(
      itemName: json['packagingName'] as String? ?? '',
      itemType: QuickReceiveItemType.packaging,
      quantityAfter: (json['quantityAfter'] as num?)?.toInt() ?? 0,
      receivedStockQuantity: receivedStockQuantity,
    );
  }
}
