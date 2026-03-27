class ClassicBluetoothStatus {
  const ClassicBluetoothStatus({
    required this.bluetoothEnabled,
    required this.missingPermissions,
    required this.bondedDeviceCount,
    this.connectedAddress,
  });

  final bool bluetoothEnabled;
  final List<String> missingPermissions;
  final int bondedDeviceCount;
  final String? connectedAddress;

  bool get hasAllPermissions => missingPermissions.isEmpty;

  factory ClassicBluetoothStatus.fromMap(Map<dynamic, dynamic> map) {
    final permissions =
        (map['missingPermissions'] as List<dynamic>? ?? const [])
            .whereType<String>()
            .toList(growable: false);

    return ClassicBluetoothStatus(
      bluetoothEnabled: map['bluetoothEnabled'] == true,
      missingPermissions: permissions,
      bondedDeviceCount: (map['bondedDeviceCount'] as num?)?.toInt() ?? 0,
      connectedAddress:
          (map['connectedAddress'] as String?)?.trim().isNotEmpty == true
              ? (map['connectedAddress'] as String).trim()
              : null,
    );
  }
}
