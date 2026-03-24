import 'package:flutter/material.dart';

import 'config/app_config.dart';
import 'screens/login_screen.dart';
import 'screens/pos_home_screen.dart';
import 'services/api_client.dart';
import 'services/auth_service.dart';
import 'services/order_service.dart';
import 'services/product_service.dart';
import 'state/session_controller.dart';

class PosApp extends StatefulWidget {
  const PosApp({super.key});

  @override
  State<PosApp> createState() => _PosAppState();
}

class _PosAppState extends State<PosApp> {
  late final SessionController _sessionController;

  @override
  void initState() {
    super.initState();

    final apiClient = ApiClient(baseUrl: AppConfig.apiBaseUrl);
    _sessionController = SessionController(
      authService: AuthService(apiClient),
      productService: ProductService(apiClient),
      orderService: OrderService(apiClient),
      defaultApiBaseUrl: AppConfig.apiBaseUrl,
      defaultDeviceCode: AppConfig.defaultDeviceCode,
    )..restoreSession();
  }

  @override
  void dispose() {
    _sessionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _sessionController,
      builder: (context, _) {
        return MaterialApp(
          title: 'NUCOSMOS POS',
          debugShowCheckedModeBanner: false,
          theme: ThemeData(
            brightness: Brightness.dark,
            useMaterial3: true,
            scaffoldBackgroundColor: const Color(0xFF08101D),
            colorScheme: ColorScheme.fromSeed(
              seedColor: const Color(0xFF1FE4FF),
              brightness: Brightness.dark,
              primary: const Color(0xFF1FE4FF),
              secondary: const Color(0xFF79B8FF),
              surface: const Color(0xFF0F1726),
            ),
          ),
          home: _buildHome(),
        );
      },
    );
  }

  Widget _buildHome() {
    if (_sessionController.bootstrapping) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    if (_sessionController.isLoggedIn) {
      return PosHomeScreen(controller: _sessionController);
    }

    return LoginScreen(controller: _sessionController);
  }
}
