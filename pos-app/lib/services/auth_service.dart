import '../models/api_envelope.dart';
import '../models/auth_models.dart';
import 'api_client.dart';

class AuthService {
  AuthService(this._apiClient);

  final ApiClient _apiClient;

  void updateBaseUrl(String baseUrl) {
    _apiClient.baseUrl = baseUrl;
  }

  Future<PinLoginResponse> pinLogin({
    required String storeCode,
    required String roleCode,
    required String pin,
    required String deviceCode,
  }) async {
    final json = await _apiClient.post(
      '/api/v1/auth/pin-login',
      body: {
        'storeCode': storeCode,
        'roleCode': roleCode,
        'pin': pin,
        'deviceCode': deviceCode,
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
}
