class AppConfig {
  static const apiBaseUrl = String.fromEnvironment(
    'POS_API_BASE_URL',
    defaultValue: 'https://nucosmos.io',
  );

  static const defaultDeviceCode = String.fromEnvironment(
    'POS_DEVICE_CODE',
    defaultValue: 'POS-TABLET-001',
  );
}
