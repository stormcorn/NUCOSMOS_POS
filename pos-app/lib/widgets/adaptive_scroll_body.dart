import 'package:flutter/material.dart';

class AdaptiveScrollBody extends StatelessWidget {
  const AdaptiveScrollBody({
    required this.children,
    required this.embedInParentScroll,
    this.padding = EdgeInsets.zero,
    super.key,
  });

  final List<Widget> children;
  final bool embedInParentScroll;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    if (embedInParentScroll) {
      return Padding(
        padding: padding,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: children,
        ),
      );
    }

    return ListView(
      padding: padding,
      children: children,
    );
  }
}
