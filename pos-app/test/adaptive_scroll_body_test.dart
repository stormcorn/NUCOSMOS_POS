import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nucosmos_pos_app/widgets/adaptive_scroll_body.dart';

void main() {
  testWidgets('uses ListView when it owns scrolling',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SizedBox(
            height: 220,
            child: AdaptiveScrollBody(
              embedInParentScroll: false,
              children: [
                for (var index = 0; index < 10; index++)
                  Container(
                    height: 72,
                    alignment: Alignment.centerLeft,
                    child: Text('Row $index'),
                  ),
                const SizedBox(
                  height: 48,
                  child: Text('Bottom action'),
                ),
              ],
            ),
          ),
        ),
      ),
    );

    expect(find.byType(ListView), findsOneWidget);

    await tester.scrollUntilVisible(
      find.text('Bottom action'),
      200,
      scrollable: find.byType(Scrollable),
    );

    expect(find.text('Bottom action'), findsOneWidget);
  });

  testWidgets('uses Column when embedded in parent scroll', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: AdaptiveScrollBody(
            embedInParentScroll: true,
            children: [
              Text('Header'),
              Text('Body'),
            ],
          ),
        ),
      ),
    );

    expect(find.byType(ListView), findsNothing);
    expect(find.byType(Column), findsOneWidget);
    expect(find.text('Header'), findsOneWidget);
    expect(find.text('Body'), findsOneWidget);
  });
}
