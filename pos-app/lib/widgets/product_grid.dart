import 'package:flutter/material.dart';

import '../models/product_summary.dart';

class ProductGrid extends StatelessWidget {
  const ProductGrid({
    required this.products,
    required this.onAddProduct,
    this.embedInParentScroll = false,
    super.key,
  });

  final List<ProductSummary> products;
  final ValueChanged<ProductSummary> onAddProduct;
  final bool embedInParentScroll;

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
          'No products available.\nPlease check inventory or API connection.',
          textAlign: TextAlign.center,
          style: TextStyle(color: Colors.white70, height: 1.7),
        ),
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final crossAxisCount = width >= 1180
            ? 5
            : width >= 760
                ? 4
                : width >= 560
                    ? 3
                    : 2;
        final compactCard = width < 920;

        return GridView.builder(
          shrinkWrap: embedInParentScroll,
          physics:
              embedInParentScroll ? const NeverScrollableScrollPhysics() : null,
          itemCount: products.length,
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: crossAxisCount,
            crossAxisSpacing: compactCard ? 12 : 16,
            mainAxisSpacing: compactCard ? 12 : 16,
            childAspectRatio: compactCard ? 0.72 : 0.78,
          ),
          itemBuilder: (context, index) {
            final product = products[index];
            return _ProductCard(
              product: product,
              onAdd: () => onAddProduct(product),
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
    required this.onAdd,
    required this.compact,
  });

  final ProductSummary product;
  final VoidCallback onAdd;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Container(
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
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: compact ? 5 : 6,
            child: Stack(
              fit: StackFit.expand,
              children: [
                _ProductImage(imageUrl: product.imageUrl),
                if (product.campaignEnabled || product.campaignActive)
                  Positioned(
                    top: 12,
                    left: 12,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 5,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFF14F1FF),
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Text(
                        product.campaignLabel?.trim().isNotEmpty == true
                            ? product.campaignLabel!
                            : 'Promo',
                        style: const TextStyle(
                          color: Color(0xFF07111C),
                          fontWeight: FontWeight.bold,
                          fontSize: 11,
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          Expanded(
            flex: compact ? 4 : 5,
            child: Padding(
              padding: EdgeInsets.fromLTRB(
                compact ? 12 : 16,
                compact ? 10 : 14,
                compact ? 12 : 16,
                compact ? 10 : 14,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    product.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: compact ? 15 : 18,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                    ),
                  ),
                  SizedBox(height: compact ? 6 : 8),
                  Expanded(
                    child: Text(
                      (product.description ?? '').trim().isEmpty
                          ? 'Freshly prepared and ready to serve.'
                          : product.description!,
                      maxLines: compact ? 2 : 3,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: const Color(0xFF9FB1C7),
                        fontSize: compact ? 11.5 : 13,
                        height: 1.45,
                      ),
                    ),
                  ),
                  SizedBox(height: compact ? 8 : 10),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          '\$${product.price.toStringAsFixed(2)}',
                          style: TextStyle(
                            fontSize: compact ? 15 : 18,
                            fontWeight: FontWeight.w800,
                            color: const Color(0xFF14F1FF),
                          ),
                        ),
                      ),
                      SizedBox(
                        height: compact ? 30 : 34,
                        child: FilledButton(
                          onPressed: onAdd,
                          style: FilledButton.styleFrom(
                            backgroundColor: const Color(0xFF14F1FF),
                            foregroundColor: const Color(0xFF07111C),
                            padding: EdgeInsets.symmetric(
                              horizontal: compact ? 12 : 16,
                              vertical: 0,
                            ),
                            shape: RoundedRectangleBorder(
                              borderRadius:
                                  BorderRadius.circular(compact ? 10 : 12),
                            ),
                          ),
                          child: Text(
                            'Add',
                            style: TextStyle(
                              fontSize: compact ? 12 : 14,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
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
