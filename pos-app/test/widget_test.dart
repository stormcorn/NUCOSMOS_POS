import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:nucosmos_pos_app/app.dart';

void main() {
  testWidgets('POS app boots to login flow', (WidgetTester tester) async {
    SharedPreferences.setMockInitialValues({});

    await tester.pumpWidget(const PosApp());
    await tester.pump();

    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
