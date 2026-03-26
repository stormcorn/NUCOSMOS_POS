import 'dart:math' as math;

import 'package:flutter/material.dart';

class PosLayoutProfile {
  const PosLayoutProfile({
    required this.size,
    required this.aspectRatio,
    required this.isLandscape,
    required this.isCompactLandscapeTablet,
    required this.wideLayout,
    required this.desktopLayout,
    required this.contentPadding,
    required this.cartWidth,
    required this.categoryWidth,
    required this.quickReceiveListHeight,
  });

  factory PosLayoutProfile.fromSize(Size size) {
    final aspectRatio = size.width / math.max(size.height, 1);
    final isLandscape = size.width >= size.height;
    final isCompactLandscapeTablet =
        isLandscape && size.width >= 820 && aspectRatio >= 1.5;
    final wideLayout = size.width >= 940 || isCompactLandscapeTablet;
    final desktopLayout = size.width >= 1220 ||
        (isLandscape && size.width >= 1080 && aspectRatio >= 1.6);

    final contentPadding = desktopLayout
        ? 20.0
        : wideLayout
            ? 16.0
            : 14.0;
    final cartWidth = desktopLayout
        ? 310.0
        : wideLayout
            ? 284.0
            : 272.0;
    final categoryWidth = desktopLayout
        ? 148.0
        : wideLayout
            ? 132.0
            : 126.0;
    final quickReceiveListHeight = isCompactLandscapeTablet
        ? (size.height * 0.34).clamp(280.0, 360.0)
        : 420.0;

    return PosLayoutProfile(
      size: size,
      aspectRatio: aspectRatio,
      isLandscape: isLandscape,
      isCompactLandscapeTablet: isCompactLandscapeTablet,
      wideLayout: wideLayout,
      desktopLayout: desktopLayout,
      contentPadding: contentPadding,
      cartWidth: cartWidth,
      categoryWidth: categoryWidth,
      quickReceiveListHeight: quickReceiveListHeight,
    );
  }

  final Size size;
  final double aspectRatio;
  final bool isLandscape;
  final bool isCompactLandscapeTablet;
  final bool wideLayout;
  final bool desktopLayout;
  final double contentPadding;
  final double cartWidth;
  final double categoryWidth;
  final double quickReceiveListHeight;
}
