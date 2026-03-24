class ApiEnvelope<T> {
  const ApiEnvelope({
    required this.success,
    required this.data,
  });

  final bool success;
  final T data;

  factory ApiEnvelope.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromJson,
  ) {
    return ApiEnvelope<T>(
      success: json['success'] as bool? ?? false,
      data: fromJson((json['data'] as Map).cast<String, dynamic>()),
    );
  }
}
