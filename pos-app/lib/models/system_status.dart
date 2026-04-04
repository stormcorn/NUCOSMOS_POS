class StorageStatus {
  const StorageStatus({
    required this.monitoredPath,
    required this.totalBytes,
    required this.usableBytes,
    required this.usedBytes,
    required this.freePercent,
    required this.level,
    required this.warningThresholdPercent,
    required this.criticalThresholdPercent,
    required this.message,
    required this.checkedAt,
  });

  final String monitoredPath;
  final int totalBytes;
  final int usableBytes;
  final int usedBytes;
  final double freePercent;
  final String level;
  final double warningThresholdPercent;
  final double criticalThresholdPercent;
  final String message;
  final DateTime? checkedAt;

  bool get isWarning => level == 'WARNING' || level == 'CRITICAL';
  bool get isCritical => level == 'CRITICAL';

  factory StorageStatus.fromJson(Map<String, dynamic> json) {
    return StorageStatus(
      monitoredPath: json['monitoredPath'] as String? ?? '',
      totalBytes: (json['totalBytes'] as num?)?.toInt() ?? 0,
      usableBytes: (json['usableBytes'] as num?)?.toInt() ?? 0,
      usedBytes: (json['usedBytes'] as num?)?.toInt() ?? 0,
      freePercent: (json['freePercent'] as num?)?.toDouble() ?? 0,
      level: json['level'] as String? ?? 'OK',
      warningThresholdPercent:
          (json['warningThresholdPercent'] as num?)?.toDouble() ?? 15,
      criticalThresholdPercent:
          (json['criticalThresholdPercent'] as num?)?.toDouble() ?? 5,
      message: json['message'] as String? ?? '',
      checkedAt: json['checkedAt'] == null
          ? null
          : DateTime.tryParse(json['checkedAt'] as String),
    );
  }
}
