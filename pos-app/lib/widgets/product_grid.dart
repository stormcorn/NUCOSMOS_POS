import 'package:flutter/material.dart';

import '../models/product_summary.dart';

class ProductGrid extends StatelessWidget {
  const ProductGrid({
    required this.products,
    required this.onAddProduct,
    this.embedInParentScroll = false,
    this.compactTabletMode = false,
    super.key,
  });

  final List<ProductSummary> products;
  final ValueChanged<ProductSummary> onAddProduct;
  final bool embedInParentScroll;
  final bool compactTabletMode;

  @override
  Widget build(BuildContext context) {
    if (products.isEmpty) {
      return Container(
        width: double.infinity,
        decoration: BoxDecoration(
          color: const Color(0xFF172132),
          borderRadius: BorderRadius.circular(28),
          border: Border.all(color: const Color(0xFF243047)),
        ),
        alignment: Alignment.center,
        child: const Text(
          '目前沒有可顯示的商品資料。\n請確認分類、商品與 API 同步狀態。',
          textAlign: TextAlign.center,
          style: TextStyle(color: Colors.white70, height: 1.7),
        ),
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final crossAxisCount = compactTabletMode
            ? 3
            : width >= 1180
                ? 5
                : width >= 760
                    ? 4
                    : width >= 560
                        ? 3
                        : 2;
        final compactCard = compactTabletMode || width < 920;

        return GridView.builder(
          shrinkWrap: embedInParentScroll,
          physics:
              embedInParentScroll ? const NeverScrollableScrollPhysics() : null,
          itemCount: products.length,
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: crossAxisCount,
            crossAxisSpacing: compactCard ? 12 : 16,
            mainAxisSpacing: compactCard ? 12 : 16,
            childAspectRatio: compactCard ? 0.84 : 0.9,
          ),
          itemBuilder: (context, index) {
            final product = products[index];
            return _ProductCard(
              product: product,
              onTap: () => onAddProduct(product),
              compact: compactCard,
            );
          },
        );
      },
    );
  }
}

class _ProductCard extends StatelessWidget {
  const _ProductCard({
    required this.product,
    required this.onTap,
    required this.compact,
  });

  final ProductSummary product;
  final VoidCallback onTap;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final soldOut = product.quantityOnHand <= 0;
    final hasCustomization = product.customizationGroups.isNotEmpty;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: soldOut ? null : onTap,
        borderRadius: BorderRadius.circular(compact ? 18 : 22),
        child: Ink(
          decoration: BoxDecoration(
            color: const Color(0xFF253043),
            borderRadius: BorderRadius.circular(compact ? 18 : 22),
            boxShadow: const [
              BoxShadow(
                color: Color(0x22000000),
                blurRadius: 16,
                offset: Offset(0, 10),
              ),
            ],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(compact ? 18 : 22),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  flex: 7,
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      _ProductImage(imageUrl: product.imageUrl),
                      const DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                            colors: [
                              Color(0x2207111C),
                              Color(0x0007111C),
                              Color(0xCC07111C),
                            ],
                          ),
                        ),
                      ),
                      Positioned(
                        top: 12,
                        left: 12,
                        child: Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: [
                            if (product.campaignEnabled || product.campaignActive)
                              _InfoChip(
                                label:
                                    product.campaignLabel?.trim().isNotEmpty == true
                                        ? product.campaignLabel!
                                        : '優惠',
                                backgroundColor: const Color(0xFF14F1FF),
                                foregroundColor: const Color(0xFF07111C),
                              ),
                            if (hasCustomization)
                              const _InfoChip(
                                label: '可客製',
                                backgroundColor: Color(0x332CF0C9),
                                foregroundColor: Color(0xFF7BF7DE),
                                borderColor: Color(0x664AF6D7),
                              ),
                          ],
                        ),
                      ),
                      Positioned(
                        left: 12,
                        right: 12,
                        bottom: 12,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              product.name,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: compact ? 16 : 18,
                                fontWeight: FontWeight.w800,
                                height: 1.15,
                              ),
                            ),
                            const SizedBox(height: 8),
                            _InfoChip(
                              label: soldOut
                                  ? '已售完'
                                  : '剩餘庫存 ${product.quantityOnHand}',
                              backgroundColor: soldOut
                                  ? const Color(0x33FF6E79)
                                  : const Color(0x1F68F3C6),
                              foregroundColor: soldOut
                                  ? const Color(0xFFFFA0A8)
                                  : const Color(0xFF68F3C6),
                              borderColor: soldOut
                                  ? const Color(0x66FF6E79)
                                  : const Color(0x3368F3C6),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  flex: 3,
                  child: Padding(
                    padding: EdgeInsets.fromLTRB(
                      compact ? 12 : 16,
                      compact ? 10 : 12,
                      compact ? 12 : 16,
                      compact ? 10 : 12,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if ((product.description ?? '').trim().isNotEmpty) ...[
                          Text(
                            product.description!,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: const Color(0xFF9FB1C7),
                              fontSize: compact ? 11.5 : 12.5,
                            ),
                          ),
                          const Spacer(),
                        ] else
                          const Spacer(),
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Expanded(
                              child: Text(
                                '\$${product.price.toStringAsFixed(2)}',
                                style: TextStyle(
                                  fontSize: compact ? 16 : 18,
                                  fontWeight: FontWeight.w800,
                                  color: const Color(0xFF14F1FF),
                                ),
                              ),
                            ),
                            Text(
                              soldOut
                                  ? '已售完'
                                  : hasCustomization
                                      ? '點擊選項後加入'
                                      : '點擊加入',
                              style: TextStyle(
                                color: soldOut
                                    ? const Color(0xFF90A2B8)
                                    : const Color(0xFFE5F7FF),
                                fontSize: compact ? 12 : 13,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _InfoChip extends StatelessWidget {
  const _InfoChip({
    required this.label,
    required this.backgroundColor,
    required this.foregroundColor,
    this.borderColor,
  });

  final String label;
  final Color backgroundColor;
  final Color foregroundColor;
  final Color? borderColor;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(999),
        border: borderColor == null ? null : Border.all(color: borderColor!),
      ),
      child: Text(
        label,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          color: foregroundColor,
          fontWeight: FontWeight.w700,
          fontSize: 11,
        ),
      ),
    );
  }
}

class _ProductImage extends StatelessWidget {
  const _ProductImage({required this.imageUrl});

  final String? imageUrl;

  @override
  Widget build(BuildContext context) {
    final url = imageUrl?.trim();
    if (url == null || url.isEmpty) {
      return Container(
        color: const Color(0xFF0C1320),
        alignment: Alignment.center,
        child: const Icon(
          Icons.image_not_supported_outlined,
          size: 46,
          color: Colors.white38,
        ),
      );
    }

    return Image.network(
      url,
      fit: BoxFit.cover,
      errorBuilder: (context, error, stackTrace) {
        return Container(
          color: const Color(0xFF0C1320),
          alignment: Alignment.center,
          child: const Icon(
            Icons.broken_image_outlined,
            size: 46,
            color: Colors.white38,
          ),
        );
      },
    );
  }
}
