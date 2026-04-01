import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

class ApiClient {
  ApiClient({required this.baseUrl});

  static const requestTimeout = Duration(seconds: 20);

  String baseUrl;

  Future<Map<String, dynamic>> get(
    String path, {
    String? accessToken,
  }) async {
    final response = await http
        .get(
          Uri.parse('${baseUrl.trim()}$path'),
          headers: _headers(accessToken: accessToken),
        )
        .timeout(requestTimeout);

    return _parseResponse(response);
  }

  Future<Map<String, dynamic>> post(
    String path, {
    Map<String, dynamic>? body,
    String? accessToken,
  }) async {
    final response = await http
        .post(
          Uri.parse('${baseUrl.trim()}$path'),
          headers: _headers(accessToken: accessToken),
          body: body == null ? null : jsonEncode(body),
        )
        .timeout(requestTimeout);

    return _parseResponse(response);
  }

  Future<Map<String, dynamic>> put(
    String path, {
    Map<String, dynamic>? body,
    String? accessToken,
  }) async {
    final response = await http
        .put(
          Uri.parse('${baseUrl.trim()}$path'),
          headers: _headers(accessToken: accessToken),
          body: body == null ? null : jsonEncode(body),
        )
        .timeout(requestTimeout);

    return _parseResponse(response);
  }

  Map<String, String> _headers({String? accessToken}) {
    return {
      'Content-Type': 'application/json',
      if (accessToken != null && accessToken.isNotEmpty)
        'Authorization': 'Bearer $accessToken',
    };
  }

  Map<String, dynamic> _parseResponse(http.Response response) {
    final json = jsonDecode(utf8.decode(response.bodyBytes));

    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = json is Map<String, dynamic>
          ? (json['message'] as String? ?? 'Request failed')
          : 'Request failed';
      throw ApiException(message, response.statusCode);
    }

    return (json as Map).cast<String, dynamic>();
  }
}

class ApiException implements Exception {
  ApiException(this.message, this.statusCode);

  final String message;
  final int statusCode;

  @override
  String toString() => 'ApiException($statusCode): $message';
}
