class ClassicBluetoothDevice {
  const ClassicBluetoothDevice({
    required this.name,
    required this.address,
    required this.bondState,
    this.isConnected = false,
  });

  final String name;
  final String address;
  final String bondState;
  final bool isConnected;

  factory ClassicBluetoothDevice.fromMap(Map<dynamic, dynamic> map) {
    return ClassicBluetoothDevice(
      name: (map['name'] as String?)?.trim().isNotEmpty == true
          ? (map['name'] as String).trim()
          : '\u672a\u547d\u540d\u85cd\u7259\u88dd\u7f6e',
      address: (map['address'] as String?)?.trim() ?? '',
      bondState: (map['bondState'] as String?)?.trim().isNotEmpty == true
          ? (map['bondState'] as String).trim()
          : 'UNKNOWN',
      isConnected: map['isConnected'] == true,
    );
  }
}
