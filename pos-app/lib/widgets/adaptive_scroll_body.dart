import 'package:flutter/material.dart';

class AdaptiveScrollBody extends StatefulWidget {
  const AdaptiveScrollBody({
    required this.children,
    required this.embedInParentScroll,
    this.padding = EdgeInsets.zero,
    this.showScrollHint = true,
    super.key,
  });

  final List<Widget> children;
  final bool embedInParentScroll;
  final EdgeInsetsGeometry padding;
  final bool showScrollHint;

  @override
  State<AdaptiveScrollBody> createState() => _AdaptiveScrollBodyState();
}

class _AdaptiveScrollBodyState extends State<AdaptiveScrollBody> {
  late final ScrollController _scrollController;
  bool _showTopFade = false;
  bool _showBottomFade = false;

  @override
  void initState() {
    super.initState();
    _scrollController = ScrollController()..addListener(_updateScrollState);
    WidgetsBinding.instance.addPostFrameCallback((_) => _updateScrollState());
  }

  @override
  void dispose() {
    _scrollController
      ..removeListener(_updateScrollState)
      ..dispose();
    super.dispose();
  }

  void _updateScrollState() {
    if (!mounted || !_scrollController.hasClients) {
      return;
    }
    final position = _scrollController.position;
    final nextTop = position.pixels > 8;
    final nextBottom = position.pixels < position.maxScrollExtent - 8;
    if (nextTop != _showTopFade || nextBottom != _showBottomFade) {
      setState(() {
        _showTopFade = nextTop;
        _showBottomFade = nextBottom;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.embedInParentScroll) {
      return Padding(
        padding: widget.padding,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: widget.children,
        ),
      );
    }

    final listView = Scrollbar(
      controller: _scrollController,
      thumbVisibility: true,
      child: ListView(
        controller: _scrollController,
        padding: widget.padding,
        children: widget.children,
      ),
    );

    return Stack(
      children: [
        listView,
        if (_showTopFade)
          const Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: IgnorePointer(
                child: _ScrollFade(alignment: Alignment.topCenter)),
          ),
        if (_showBottomFade)
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: IgnorePointer(
              child: _ScrollFade(
                alignment: Alignment.bottomCenter,
                showHint: widget.showScrollHint,
              ),
            ),
          ),
      ],
    );
  }
}

class _ScrollFade extends StatelessWidget {
  const _ScrollFade({
    required this.alignment,
    this.showHint = false,
  });

  final Alignment alignment;
  final bool showHint;

  @override
  Widget build(BuildContext context) {
    final isTop = alignment == Alignment.topCenter;
    return Container(
      height: showHint ? 54 : 24,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: isTop ? Alignment.topCenter : Alignment.bottomCenter,
          end: isTop ? Alignment.bottomCenter : Alignment.topCenter,
          colors: const [
            Color(0xFF0C1118),
            Color(0x000C1118),
          ],
        ),
      ),
      alignment: isTop ? Alignment.topCenter : Alignment.bottomCenter,
      padding: EdgeInsets.only(bottom: showHint && !isTop ? 8 : 0),
      child: showHint && !isTop
          ? Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: const Color(0xD9151D2C),
                borderRadius: BorderRadius.circular(999),
                border: Border.all(color: const Color(0xFF253043)),
              ),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.swipe_up_alt_rounded,
                      size: 14, color: Colors.white70),
                  SizedBox(width: 4),
                  Text(
                    '向下滑動查看更多',
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            )
          : null,
    );
  }
}
