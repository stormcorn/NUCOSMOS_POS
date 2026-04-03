import '../models/api_envelope.dart';
import '../models/quick_receive_models.dart';
import 'api_client.dart';

class QuickReceiveService {
  QuickReceiveService(this._apiClient);

  final ApiClient _apiClient;

  void updateBaseUrl(String baseUrl) {
    _apiClient.baseUrl = baseUrl;
  }

  Future<List<QuickReceiveItem>> fetchMaterials(String accessToken) async {
    return _fetchItems(
      accessToken: accessToken,
      path: QuickReceiveItemType.material.catalogPath,
      mapper: QuickReceiveItem.fromMaterialJson,
    );
  }

  Future<List<QuickReceiveItem>> fetchManufacturedItems(
    String accessToken,
  ) async {
    return _fetchItems(
      accessToken: accessToken,
      path: QuickReceiveItemType.manufactured.catalogPath,
      mapper: QuickReceiveItem.fromManufacturedJson,
    );
  }

  Future<List<QuickReceiveItem>> fetchPackagingItems(String accessToken) async {
    return _fetchItems(
      accessToken: accessToken,
      path: QuickReceiveItemType.packaging.catalogPath,
      mapper: QuickReceiveItem.fromPackagingJson,
    );
  }

  Future<List<QuickReceiveItem>> fetchProductStocks(String accessToken) async {
    return _fetchItems(
      accessToken: accessToken,
      path: QuickReceiveItemType.product.catalogPath,
      mapper: QuickReceiveItem.fromProductStockJson,
    );
  }

  Future<QuickReceiveItem> createItem({
    required String accessToken,
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
    final createPath = type.createPath;
    if (createPath == null) {
      throw ApiException('${type.label}請先在後台建立，再到 POS 進行入庫。', 400);
    }

    final payload = {
      'sku': sku.trim(),
      'name': name.trim(),
      'unit': unit.trim(),
      'purchaseUnit': purchaseUnit.trim(),
      'purchaseToStockRatio': purchaseToStockRatio,
      'description': description?.trim() ?? '',
      'reorderLevel': reorderLevel,
      'latestUnitCost': latestUnitCost,
      if (type == QuickReceiveItemType.packaging) 'specification': '',
      'imageUrl': '',
    };

    final json = await _apiClient.post(
      createPath,
      accessToken: accessToken,
      body: payload,
    );

    return ApiEnvelope<QuickReceiveItem>.fromJson(
      json,
      (data) => _mapItem(type, data),
    ).data;
  }

  Future<QuickReceiveResult> submitQuickReceive({
    required String accessToken,
    required QuickReceiveItem item,
    required int purchaseQuantity,
    double? purchaseUnitCost,
    String? note,
  }) async {
    final receivedStockQuantity = purchaseQuantity * item.purchaseToStockRatio;
    final unitCost = purchaseUnitCost == null
        ? null
        : purchaseUnitCost / item.purchaseToStockRatio;

    if (item.type == QuickReceiveItemType.product) {
      final json = await _apiClient.post(
        '/api/v1/admin/inventory/movements',
        accessToken: accessToken,
        body: {
          'productId': item.id,
          'movementType': 'PURCHASE_IN',
          'quantity': purchaseQuantity,
          if (purchaseUnitCost != null)
            'unitCost': double.parse(purchaseUnitCost.toStringAsFixed(2)),
          if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
        },
      );

      return ApiEnvelope<QuickReceiveResult>.fromJson(
        json,
        (data) => QuickReceiveResult.fromProductMovementJson(
          data,
          receivedStockQuantity: purchaseQuantity,
        ),
      ).data;
    }

    final payload = {
      'movementType': 'PURCHASE_IN',
      'quantity': receivedStockQuantity,
      if (unitCost != null)
        'unitCost': double.parse(unitCost.toStringAsFixed(2)),
      if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
    };

    final json = await _apiClient.post(
      '${item.type.catalogPath}/${item.id}/movements',
      accessToken: accessToken,
      body: payload,
    );

    return ApiEnvelope<QuickReceiveResult>.fromJson(
      json,
      (data) => _mapResult(
        item.type,
        data,
        receivedStockQuantity: receivedStockQuantity,
      ),
    ).data;
  }

  Future<List<QuickReceiveItem>> _fetchItems({
    required String accessToken,
    required String path,
    required QuickReceiveItem Function(Map<String, dynamic> json) mapper,
  }) async {
    final json = await _apiClient.get(path, accessToken: accessToken);
    final data = (json['data'] as List?) ?? <dynamic>[];
    return data
        .map((item) => mapper((item as Map).cast<String, dynamic>()))
        .toList(growable: false);
  }

  QuickReceiveItem _mapItem(
    QuickReceiveItemType type,
    Map<String, dynamic> json,
  ) {
    switch (type) {
      case QuickReceiveItemType.material:
        return QuickReceiveItem.fromMaterialJson(json);
      case QuickReceiveItemType.manufactured:
        return QuickReceiveItem.fromManufacturedJson(json);
      case QuickReceiveItemType.packaging:
        return QuickReceiveItem.fromPackagingJson(json);
      case QuickReceiveItemType.product:
        return QuickReceiveItem.fromProductStockJson(json);
    }
  }

  QuickReceiveResult _mapResult(
    QuickReceiveItemType type,
    Map<String, dynamic> json, {
    required int receivedStockQuantity,
  }) {
    switch (type) {
      case QuickReceiveItemType.material:
        return QuickReceiveResult.fromMaterialJson(
          json,
          receivedStockQuantity: receivedStockQuantity,
        );
      case QuickReceiveItemType.manufactured:
        return QuickReceiveResult.fromManufacturedJson(
          json,
          receivedStockQuantity: receivedStockQuantity,
        );
      case QuickReceiveItemType.packaging:
        return QuickReceiveResult.fromPackagingJson(
          json,
          receivedStockQuantity: receivedStockQuantity,
        );
      case QuickReceiveItemType.product:
        return QuickReceiveResult.fromProductMovementJson(
          json,
          receivedStockQuantity: receivedStockQuantity,
        );
    }
  }
}
