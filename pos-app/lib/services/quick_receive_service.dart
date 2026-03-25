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
    final json = await _apiClient.get(
      '/api/v1/admin/materials',
      accessToken: accessToken,
    );
    final data = (json['data'] as List?) ?? <dynamic>[];
    return data
        .map(
          (item) => QuickReceiveItem.fromMaterialJson(
            (item as Map).cast<String, dynamic>(),
          ),
        )
        .toList(growable: false);
  }

  Future<List<QuickReceiveItem>> fetchPackagingItems(String accessToken) async {
    final json = await _apiClient.get(
      '/api/v1/admin/packaging-items',
      accessToken: accessToken,
    );
    final data = (json['data'] as List?) ?? <dynamic>[];
    return data
        .map(
          (item) => QuickReceiveItem.fromPackagingJson(
            (item as Map).cast<String, dynamic>(),
          ),
        )
        .toList(growable: false);
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
      if (unitCost != null) 'unitCost': double.parse(unitCost.toStringAsFixed(2)),
      if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
    };

    final json = await _apiClient.post(
      '${item.type.apiPath}/${item.id}/movements',
      accessToken: accessToken,
      body: payload,
    );

    if (item.type == QuickReceiveItemType.material) {
      return ApiEnvelope<QuickReceiveResult>.fromJson(
        json,
        (data) => QuickReceiveResult.fromMaterialJson(
          data,
          receivedStockQuantity: receivedStockQuantity,
        ),
      ).data;
    }

    return ApiEnvelope<QuickReceiveResult>.fromJson(
      json,
      (data) => QuickReceiveResult.fromPackagingJson(
        data,
        receivedStockQuantity: receivedStockQuantity,
      ),
    ).data;
  }
}
