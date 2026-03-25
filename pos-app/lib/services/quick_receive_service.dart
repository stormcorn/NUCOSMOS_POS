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
      path: '/api/v1/admin/materials',
      mapper: QuickReceiveItem.fromMaterialJson,
    );
  }

  Future<List<QuickReceiveItem>> fetchManufacturedItems(
    String accessToken,
  ) async {
    return _fetchItems(
      accessToken: accessToken,
      path: '/api/v1/admin/manufactured-items',
      mapper: QuickReceiveItem.fromManufacturedJson,
    );
  }

  Future<List<QuickReceiveItem>> fetchPackagingItems(String accessToken) async {
    return _fetchItems(
      accessToken: accessToken,
      path: '/api/v1/admin/packaging-items',
      mapper: QuickReceiveItem.fromPackagingJson,
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
      type.apiPath,
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
    final payload = {
      'movementType': 'PURCHASE_IN',
      'quantity': receivedStockQuantity,
      if (unitCost != null)
        'unitCost': double.parse(unitCost.toStringAsFixed(2)),
      if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
    };

    final json = await _apiClient.post(
      '${item.type.apiPath}/${item.id}/movements',
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
    }
  }
}
