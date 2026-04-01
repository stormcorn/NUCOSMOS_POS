import '../models/api_envelope.dart';
import '../models/auth_models.dart';
import 'api_client.dart';

class AuthService {
  AuthService(this._apiClient);

  final ApiClient _apiClient;

  void updateBaseUrl(String baseUrl) {
    _apiClient.baseUrl = baseUrl;
  }

  Future<List<StoreSummary>> fetchStores() async {
    final json = await _apiClient.get('/api/v1/auth/stores');
    return ApiEnvelope<List<dynamic>>.fromJson(
      json,
      (items) => items as List<dynamic>,
    ).data.map((item) {
      return StoreSummary.fromJson(
        (item as Map).cast<String, dynamic>(),
      );
    }).toList(growable: false);
  }

  Future<PinLoginResponse> pinLogin({
    required String storeCode,
    required String pin,
    required String deviceCode,
    String? roleCode,
    String? deviceName,
    String? devicePlatform,
  }) async {
    final json = await _apiClient.post(
      '/api/v1/auth/pin-login',
      body: {
        'storeCode': storeCode,
        if (roleCode != null && roleCode.isNotEmpty) 'roleCode': roleCode,
        'pin': pin,
        'deviceCode': deviceCode,
        if (deviceName != null && deviceName.isNotEmpty) 'deviceName': deviceName,
        if (devicePlatform != null && devicePlatform.isNotEmpty)
          'devicePlatform': devicePlatform,
      },
    );

    return ApiEnvelope<PinLoginResponse>.fromJson(
      json,
      PinLoginResponse.fromJson,
    ).data;
  }

  Future<CurrentSession> currentSession(String accessToken) async {
    final json = await _apiClient.get(
      '/api/v1/auth/me',
      accessToken: accessToken,
    );

    return ApiEnvelope<CurrentSession>.fromJson(
      json,
      CurrentSession.fromJson,
    ).data;
  }

  Future<void> healthCheck() async {
    await _apiClient.get('/api/v1/health');
  }

  Future<StoreReceiptSettings> fetchCurrentStoreReceiptSettings(
    String accessToken,
  ) async {
    final json = await _apiClient.get(
      '/api/v1/auth/stores/current/receipt-settings',
      accessToken: accessToken,
    );

    return ApiEnvelope<StoreReceiptSettings>.fromJson(
      json,
      StoreReceiptSettings.fromJson,
    ).data;
  }

  Future<StoreReceiptSettings> updateCurrentStoreReceiptSettings({
    required String accessToken,
    required String receiptFooterText,
  }) async {
    final json = await _apiClient.put(
      '/api/v1/auth/stores/current/receipt-settings',
      accessToken: accessToken,
      body: {
        'receiptFooterText': receiptFooterText,
      },
    );

    return ApiEnvelope<StoreReceiptSettings>.fromJson(
      json,
      StoreReceiptSettings.fromJson,
    ).data;
  }
}
