class PrinterTarget {
  const PrinterTarget({
    required this.name,
    required this.connectionType,
    this.address,
    this.vendorId,
    this.productId,
  });

  final String name;
  final String connectionType;
  final String? address;
  final String? vendorId;
  final String? productId;

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'connectionType': connectionType,
      'address': address,
      'vendorId': vendorId,
      'productId': productId,
    };
  }

  factory PrinterTarget.fromJson(Map<String, dynamic> json) {
    return PrinterTarget(
      name: json['name']?.toString() ?? 'Unnamed Printer',
      connectionType: json['connectionType']?.toString() ?? 'UNKNOWN',
      address: json['address']?.toString(),
      vendorId: json['vendorId']?.toString(),
      productId: json['productId']?.toString(),
    );
  }
}
