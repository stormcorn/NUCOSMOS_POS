import 'dart:math' as math;

import 'package:flutter/material.dart';

class PosLayoutProfile {
  const PosLayoutProfile({
    required this.size,
    required this.aspectRatio,
    required this.isLandscape,
    required this.canvasWidth,
    required this.isCompactLandscapeTablet,
    required this.wideLayout,
    required this.desktopLayout,
    required this.contentPadding,
    required this.cartWidth,
    required this.categoryWidth,
    required this.quickReceiveListWidth,
    required this.quickReceiveListHeight,
  });

  factory PosLayoutProfile.fromSize(Size size) {
    const navigationRailWidth = 72.0;
    const tabletAspectRatio = 16 / 10;
    final aspectRatio = size.width / math.max(size.height, 1);
    final isLandscape = size.width >= size.height;
    final canvasWidth = isLandscape
        ? math.min(
            size.width,
            math.max(980.0, size.height * tabletAspectRatio),
          )
        : size.width;
    final workspaceWidth = math.max(canvasWidth - navigationRailWidth, 1);
    final isCompactLandscapeTablet =
        isLandscape && aspectRatio >= 1.5 && workspaceWidth >= 820 && workspaceWidth < 1220;
    final wideLayout = workspaceWidth >= 940 || isCompactLandscapeTablet;
    final desktopLayout = workspaceWidth >= 1220 ||
        (isLandscape && workspaceWidth >= 1080 && aspectRatio >= 1.6);

    final contentPadding = desktopLayout
        ? 20.0
        : isCompactLandscapeTablet
            ? 14.0
        : wideLayout
            ? 16.0
            : 14.0;
    final cartWidth = desktopLayout
        ? 310.0
        : isCompactLandscapeTablet
            ? 276.0
        : wideLayout
            ? 284.0
            : 272.0;
    final categoryWidth = desktopLayout
        ? 148.0
        : isCompactLandscapeTablet
            ? 124.0
        : wideLayout
            ? 132.0
            : 126.0;
    final quickReceiveListWidth = desktopLayout
        ? 390.0
        : isCompactLandscapeTablet
            ? 320.0
            : 342.0;
    final quickReceiveListHeight = isCompactLandscapeTablet
        ? (size.height * 0.36).clamp(300.0, 380.0)
        : 420.0;

    return PosLayoutProfile(
      size: size,
      aspectRatio: aspectRatio,
      isLandscape: isLandscape,
      canvasWidth: canvasWidth,
      isCompactLandscapeTablet: isCompactLandscapeTablet,
      wideLayout: wideLayout,
      desktopLayout: desktopLayout,
      contentPadding: contentPadding,
      cartWidth: cartWidth,
      categoryWidth: categoryWidth,
      quickReceiveListWidth: quickReceiveListWidth,
      quickReceiveListHeight: quickReceiveListHeight,
    );
  }

  final Size size;
  final double aspectRatio;
  final bool isLandscape;
  final double canvasWidth;
  final bool isCompactLandscapeTablet;
  final bool wideLayout;
  final bool desktopLayout;
  final double contentPadding;
  final double cartWidth;
  final double categoryWidth;
  final double quickReceiveListWidth;
  final double quickReceiveListHeight;
}
