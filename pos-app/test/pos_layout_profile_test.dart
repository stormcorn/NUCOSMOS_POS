import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:nucosmos_pos_app/models/pos_layout_profile.dart';

void main() {
  test('treats landscape tablet aspect ratios as wide layout', () {
    final profile = PosLayoutProfile.fromSize(const Size(1280, 800));

    expect(profile.isLandscape, isTrue);
    expect(profile.isCompactLandscapeTablet, isTrue);
    expect(profile.wideLayout, isTrue);
    expect(profile.quickReceiveListHeight, inInclusiveRange(280.0, 360.0));
  });

  test('keeps narrow phone layouts stacked', () {
    final profile = PosLayoutProfile.fromSize(const Size(430, 932));

    expect(profile.isLandscape, isFalse);
    expect(profile.isCompactLandscapeTablet, isFalse);
    expect(profile.wideLayout, isFalse);
    expect(profile.quickReceiveListHeight, 420.0);
  });
}
