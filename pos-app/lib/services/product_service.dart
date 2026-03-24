import '../models/product_summary.dart';
import 'api_client.dart';

class ProductService {
  ProductService(this._apiClient);

  final ApiClient _apiClient;

  void updateBaseUrl(String baseUrl) {
    _apiClient.baseUrl = baseUrl;
  }

  Future<List<ProductSummary>> fetchProducts(String accessToken) async {
    final json = await _apiClient.get(
      '/api/v1/products',
      accessToken: accessToken,
    );

    final data = (json['data'] as List?) ?? <dynamic>[];
    return data
        .map((item) => ProductSummary.fromJson((item as Map).cast<String, dynamic>()))
        .toList();
  }
}
