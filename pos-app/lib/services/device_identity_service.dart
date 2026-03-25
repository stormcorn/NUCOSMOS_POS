import 'package:device_info_plus/device_info_plus.dart';

import '../config/app_config.dart';

class PosDeviceIdentity {
  const PosDeviceIdentity({
    required this.deviceCode,
    required this.deviceName,
    required this.devicePlatform,
    required this.deviceSummary,
  });

  final String deviceCode;
  final String deviceName;
  final String devicePlatform;
  final String deviceSummary;
}

class DeviceIdentityService {
  DeviceIdentityService({DeviceInfoPlugin? plugin})
      : _plugin = plugin ?? DeviceInfoPlugin();

  final DeviceInfoPlugin _plugin;

  Future<PosDeviceIdentity> resolveIdentity() async {
    try {
      final info = await _plugin.androidInfo;
      final manufacturer = _clean(info.manufacturer);
      final model = _clean(info.model);
      final device = _clean(info.device);
      final buildId = _clean(info.id);
      final fingerprint = _clean(info.fingerprint);

      final rawSignature = [
        manufacturer,
        model,
        device,
        buildId,
        fingerprint,
      ].where((value) => value.isNotEmpty).join('|');

      final summary = [manufacturer, model]
          .where((value) => value.isNotEmpty)
          .join(' ');

      return PosDeviceIdentity(
        deviceCode: 'POS-${_stableHash(rawSignature)}',
        deviceName: summary.isEmpty ? 'Android Tablet' : summary,
        devicePlatform: 'ANDROID',
        deviceSummary: summary.isEmpty
            ? 'Android Tablet'
            : '$summary${device.isEmpty ? '' : ' · $device'}',
      );
    } catch (_) {
      return const PosDeviceIdentity(
        deviceCode: AppConfig.defaultDeviceCode,
        deviceName: 'POS Tablet',
        devicePlatform: 'ANDROID',
        deviceSummary: 'POS Tablet',
      );
    }
  }

  String _clean(String? value) => (value ?? '').trim();

  String _stableHash(String raw) {
    if (raw.isEmpty) {
      return 'TABLET001';
    }

    var hash = 2166136261;
    for (final codeUnit in raw.codeUnits) {
      hash ^= codeUnit;
      hash = (hash * 16777619) & 0xFFFFFFFF;
    }

    return hash.toRadixString(16).padLeft(8, '0').toUpperCase();
  }
}
