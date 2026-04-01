class StoreSummary {
  const StoreSummary({
    required this.code,
    required this.name,
    this.receiptFooterText = '',
  });

  final String code;
  final String name;
  final String receiptFooterText;

  factory StoreSummary.fromJson(Map<String, dynamic> json) {
    return StoreSummary(
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? '',
      receiptFooterText: json['receiptFooterText'] as String? ?? '',
    );
  }
}

class StoreReceiptSettings {
  const StoreReceiptSettings({
    required this.storeCode,
    required this.storeName,
    required this.receiptFooterText,
  });

  final String storeCode;
  final String storeName;
  final String receiptFooterText;

  factory StoreReceiptSettings.fromJson(Map<String, dynamic> json) {
    return StoreReceiptSettings(
      storeCode: json['storeCode'] as String? ?? '',
      storeName: json['storeName'] as String? ?? '',
      receiptFooterText: json['receiptFooterText'] as String? ?? '',
    );
  }
}

class StaffSummary {
  const StaffSummary({
    required this.id,
    required this.employeeCode,
    required this.displayName,
    required this.roleCodes,
    required this.activeRole,
    required this.permissionKeys,
  });

  final String id;
  final String employeeCode;
  final String displayName;
  final List<String> roleCodes;
  final String activeRole;
  final List<String> permissionKeys;

  factory StaffSummary.fromJson(Map<String, dynamic> json) {
    return StaffSummary(
      id: json['id']?.toString() ?? '',
      employeeCode: json['employeeCode'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      roleCodes: ((json['roleCodes'] as List?) ?? [])
          .map((item) => item.toString())
          .toList(),
      activeRole: json['activeRole'] as String? ?? '',
      permissionKeys: ((json['permissionKeys'] as List?) ?? [])
          .map((item) => item.toString())
          .toList(),
    );
  }
}

class PinLoginResponse {
  const PinLoginResponse({
    required this.tokenType,
    required this.accessToken,
    required this.expiresAt,
    required this.deviceCode,
    required this.store,
    required this.staff,
  });

  final String tokenType;
  final String accessToken;
  final DateTime? expiresAt;
  final String deviceCode;
  final StoreSummary store;
  final StaffSummary staff;

  factory PinLoginResponse.fromJson(Map<String, dynamic> json) {
    return PinLoginResponse(
      tokenType: json['tokenType'] as String? ?? 'Bearer',
      accessToken: json['accessToken'] as String? ?? '',
      expiresAt: json['expiresAt'] == null
          ? null
          : DateTime.tryParse(json['expiresAt'] as String),
      deviceCode: json['deviceCode'] as String? ?? '',
      store: StoreSummary.fromJson(
        (json['store'] as Map?)?.cast<String, dynamic>() ?? <String, dynamic>{},
      ),
      staff: StaffSummary.fromJson(
        (json['staff'] as Map?)?.cast<String, dynamic>() ?? <String, dynamic>{},
      ),
    );
  }
}

class CurrentSession {
  const CurrentSession({
    required this.userId,
    required this.employeeCode,
    required this.displayName,
    required this.storeCode,
    required this.activeRole,
    required this.roleCodes,
    required this.permissionKeys,
    required this.deviceCode,
  });

  final String userId;
  final String employeeCode;
  final String displayName;
  final String storeCode;
  final String activeRole;
  final List<String> roleCodes;
  final List<String> permissionKeys;
  final String deviceCode;

  factory CurrentSession.fromJson(Map<String, dynamic> json) {
    return CurrentSession(
      userId: json['userId']?.toString() ?? '',
      employeeCode: json['employeeCode'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      storeCode: json['storeCode'] as String? ?? '',
      activeRole: json['activeRole'] as String? ?? '',
      roleCodes: ((json['roleCodes'] as List?) ?? [])
          .map((item) => item.toString())
          .toList(),
      permissionKeys: ((json['permissionKeys'] as List?) ?? [])
          .map((item) => item.toString())
          .toList(),
      deviceCode: json['deviceCode'] as String? ?? '',
    );
  }
}
