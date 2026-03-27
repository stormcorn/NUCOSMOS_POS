class ClassicBluetoothDevice {
  const ClassicBluetoothDevice({
    required this.name,
    required this.address,
    required this.bondState,
  });

  final String name;
  final String address;
  final String bondState;

  factory ClassicBluetoothDevice.fromMap(Map<dynamic, dynamic> map) {
    return ClassicBluetoothDevice(
      name: (map['name'] as String?)?.trim().isNotEmpty == true
          ? (map['name'] as String).trim()
          : '未命名藍牙裝置',
      address: (map['address'] as String?)?.trim() ?? '',
      bondState: (map['bondState'] as String?)?.trim().isNotEmpty == true
          ? (map['bondState'] as String).trim()
          : 'UNKNOWN',
    );
  }
}
