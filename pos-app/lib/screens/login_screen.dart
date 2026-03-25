import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../models/auth_models.dart';
import '../state/session_controller.dart';
import '../widgets/pin_pad.dart';

const _pinDigits = 4;
const _tabletAspectRatio = 16 / 10;

class LoginScreen extends StatefulWidget {
  const LoginScreen({
    required this.controller,
    super.key,
  });

  final SessionController controller;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  late final TextEditingController _apiBaseUrlController;

  String _pin = '';
  bool _testingConnection = false;

  @override
  void initState() {
    super.initState();
    _apiBaseUrlController = TextEditingController(
      text: widget.controller.apiBaseUrl,
    );
  }

  @override
  void dispose() {
    _apiBaseUrlController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    await widget.controller.updateApiBaseUrl(_apiBaseUrlController.text.trim());

    final storeCode = widget.controller.selectedStoreCode;
    if (storeCode == null || storeCode.isEmpty) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('請先選擇門市')),
      );
      return;
    }

    final success = await widget.controller.login(
      storeCode: storeCode,
      pin: _pin,
    );

    if (!mounted) {
      return;
    }

    if (!success && widget.controller.errorMessage.isNotEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(widget.controller.errorMessage)),
      );
      return;
    }

    if (success) {
      setState(() {
        _pin = '';
      });
    }
  }

  Future<void> _testConnection() async {
    FocusScope.of(context).unfocus();
    setState(() {
      _testingConnection = true;
    });

    await widget.controller.updateApiBaseUrl(_apiBaseUrlController.text.trim());
    final success = await widget.controller.testConnection();

    if (!mounted) {
      return;
    }

    setState(() {
      _testingConnection = false;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          success
              ? '連線成功：${widget.controller.apiBaseUrl}'
              : widget.controller.errorMessage,
        ),
      ),
    );
  }

  void _appendDigit(String digit) {
    if (_pin.length >= _pinDigits) {
      return;
    }

    setState(() {
      _pin += digit;
    });
  }

  void _removeDigit() {
    if (_pin.isEmpty) {
      return;
    }

    setState(() {
      _pin = _pin.substring(0, _pin.length - 1);
    });
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.controller,
      builder: (context, _) {
        final theme = Theme.of(context);

        return Scaffold(
          body: SafeArea(
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 1100),
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: LayoutBuilder(
                    builder: (context, constraints) {
                      final isTabletWide = constraints.maxWidth >= 840;
                      final loginPanel = _LoginPanel(
                        theme: theme,
                        stores: widget.controller.availableStores,
                        selectedStoreCode: widget.controller.selectedStoreCode,
                        deviceCode: widget.controller.deviceCode,
                        deviceSummary: widget.controller.deviceSummary,
                        apiBaseUrlController: _apiBaseUrlController,
                        pin: _pin,
                        loading: widget.controller.loading,
                        testingConnection: _testingConnection,
                        errorMessage: widget.controller.errorMessage,
                        currentApiBaseUrl: widget.controller.apiBaseUrl,
                        onStoreChanged: (value) {
                          widget.controller.updateSelectedStoreCode(value);
                        },
                        onSubmit: _submit,
                        onTestConnection: _testConnection,
                      );
                      final pinPad = PinPad(
                        maxPinLength: _pinDigits,
                        pinLength: _pin.length,
                        loading: widget.controller.loading,
                        onDigitTap: _appendDigit,
                        onBackspace: _removeDigit,
                        onSubmit: _pin.length == _pinDigits ? _submit : null,
                      );

                      if (isTabletWide) {
                        final targetWidth = math.min(
                          constraints.maxWidth,
                          constraints.maxHeight * _tabletAspectRatio,
                        );
                        final targetHeight = targetWidth / _tabletAspectRatio;
                        final keypadWidth = targetWidth >= 1024 ? 360.0 : 320.0;
                        final leftPanelWidth = targetWidth - keypadWidth - 24;

                        return Center(
                          child: SizedBox(
                            width: targetWidth,
                            height: targetHeight,
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                SizedBox(
                                  width: leftPanelWidth,
                                  height: targetHeight,
                                  child: loginPanel,
                                ),
                                const SizedBox(width: 24),
                                SizedBox(
                                  width: keypadWidth,
                                  height: targetHeight,
                                  child: pinPad,
                                ),
                              ],
                            ),
                          ),
                        );
                      }

                      return SingleChildScrollView(
                        child: Column(
                          children: [
                            loginPanel,
                            const SizedBox(height: 24),
                            SizedBox(
                              height: 440,
                              child: pinPad,
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

class _LoginPanel extends StatelessWidget {
  const _LoginPanel({
    required this.theme,
    required this.stores,
    required this.selectedStoreCode,
    required this.deviceCode,
    required this.deviceSummary,
    required this.apiBaseUrlController,
    required this.pin,
    required this.loading,
    required this.testingConnection,
    required this.errorMessage,
    required this.currentApiBaseUrl,
    required this.onStoreChanged,
    required this.onSubmit,
    required this.onTestConnection,
  });

  final ThemeData theme;
  final List<StoreSummary> stores;
  final String? selectedStoreCode;
  final String deviceCode;
  final String deviceSummary;
  final TextEditingController apiBaseUrlController;
  final String pin;
  final bool loading;
  final bool testingConnection;
  final String errorMessage;
  final String currentApiBaseUrl;
  final ValueChanged<String?> onStoreChanged;
  final VoidCallback onSubmit;
  final VoidCallback onTestConnection;

  @override
  Widget build(BuildContext context) {
    final selectedValue = stores.any((store) => store.code == selectedStoreCode)
        ? selectedStoreCode
        : null;

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: const Color(0xFF0E1726),
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: const Color(0xFF1D2A42)),
      ),
      child: SingleChildScrollView(
        physics: const BouncingScrollPhysics(),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'NUCOSMOS POS',
              style: theme.textTheme.headlineMedium?.copyWith(
                color: const Color(0xFF1FE4FF),
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '輸入 PIN 後自動辨識帳號與身分組',
              style: theme.textTheme.titleMedium?.copyWith(
                color: Colors.white70,
              ),
            ),
            const SizedBox(height: 20),
            Text(
              '目前 API：$currentApiBaseUrl',
              style: const TextStyle(color: Colors.white60),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: apiBaseUrlController,
              decoration: const InputDecoration(
                labelText: 'API Base URL',
                hintText: 'https://nucosmos.io',
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: testingConnection ? null : onTestConnection,
                    icon: testingConnection
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.wifi_tethering_rounded),
                    label: Text(testingConnection ? '測試中...' : '測試連線'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            DropdownButtonFormField<String>(
              initialValue: selectedValue,
              decoration: const InputDecoration(
                labelText: 'Store Code',
              ),
              items: stores
                  .map(
                    (store) => DropdownMenuItem<String>(
                      value: store.code,
                      child: Text('${store.code} · ${store.name}'),
                    ),
                  )
                  .toList(growable: false),
              onChanged: loading ? null : onStoreChanged,
            ),
            const SizedBox(height: 16),
            _DeviceCard(
              deviceCode: deviceCode,
              deviceSummary: deviceSummary,
            ),
            const SizedBox(height: 24),
            Text(
              'PIN',
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: List.generate(_pinDigits, (index) {
                final filled = index < pin.length;
                return Expanded(
                  child: Container(
                    height: 56,
                    margin: EdgeInsets.only(
                      right: index == _pinDigits - 1 ? 0 : 10,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFF08101D),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                        color: filled
                            ? const Color(0xFF1FE4FF)
                            : const Color(0xFF2A3856),
                      ),
                    ),
                    alignment: Alignment.center,
                    child: Text(
                      filled ? '•' : '',
                      style: const TextStyle(
                        fontSize: 28,
                        color: Colors.white,
                      ),
                    ),
                  ),
                );
              }),
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: loading || pin.length != _pinDigits ? null : onSubmit,
                icon: loading
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.login_rounded),
                label: Text(
                  loading ? '登入中...' : '輸入 4 碼 PIN 登入',
                ),
              ),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: const Color(0xFF08101D),
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: const Color(0xFF22314B)),
              ),
              child: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '登入規則',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                  SizedBox(height: 10),
                  Text('1. 先選門市，再輸入 4 碼 PIN。'),
                  Text('2. 系統會自動判斷員工帳號與可用身分組。'),
                  Text('3. 平板裝置碼會由系統自動讀取並帶入。'),
                ],
              ),
            ),
            if (errorMessage.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text(
                errorMessage,
                style: const TextStyle(color: Colors.redAccent),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _DeviceCard extends StatelessWidget {
  const _DeviceCard({
    required this.deviceCode,
    required this.deviceSummary,
  });

  final String deviceCode;
  final String deviceSummary;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF08101D),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFF22314B)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '目前裝置',
            style: TextStyle(
              color: Colors.white70,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            deviceSummary,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            deviceCode,
            style: const TextStyle(color: Colors.white60),
          ),
        ],
      ),
    );
  }
}
