import 'package:flutter/material.dart';

import '../models/product_summary.dart';

class ProductGrid extends StatelessWidget {
  const ProductGrid({
    required this.products,
    required this.onAddProduct,
    super.key,
  });

  final List<ProductSummary> products;
  final ValueChanged<ProductSummary> onAddProduct;

  @override
  Widget build(BuildContext context) {
    if (products.isEmpty) {
      return Container(
        width: double.infinity,
        decoration: BoxDecoration(
          color: const Color(0xFF0E1726),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: const Color(0xFF22314B)),
        ),
        alignment: Alignment.center,
        child: const Text(
          '目前沒有可販售商品。\n請先確認商品資料與 API 連線是否正常。',
          textAlign: TextAlign.center,
          style: TextStyle(color: Colors.white70, height: 1.6),
        ),
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final crossAxisCount = constraints.maxWidth >= 1200
            ? 4
            : constraints.maxWidth >= 800
                ? 3
                : 2;

        return GridView.builder(
          itemCount: products.length,
          gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: crossAxisCount,
            mainAxisSpacing: 16,
            crossAxisSpacing: 16,
            childAspectRatio: 0.92,
          ),
          itemBuilder: (context, index) {
            final product = products[index];
            return _ProductCard(
              product: product,
              onAdd: () => onAddProduct(product),
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
  });

  final ProductSummary product;
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF0E1726),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFF22314B)),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
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
                        horizontal: 10,
                        vertical: 6,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFF1FE4FF),
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Text(
                        product.campaignLabel?.trim().isNotEmpty == true
                            ? product.campaignLabel!
                            : '活動商品',
                        style: const TextStyle(
                          color: Color(0xFF08101D),
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  product.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  product.categoryName,
                  style: const TextStyle(color: Colors.white70),
                ),
                const SizedBox(height: 10),
                if ((product.description ?? '').isNotEmpty)
                  Text(
                    product.description!,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: Colors.white60, height: 1.4),
                  )
                else
                  const Text(
                    '暫無商品說明',
                    style: TextStyle(color: Colors.white38),
                  ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _formatPrice(product.price),
                            style: const TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.bold,
                              color: Color(0xFF1FE4FF),
                            ),
                          ),
                          if (product.originalPrice > product.price) ...[
                            const SizedBox(height: 4),
                            Text(
                              _formatPrice(product.originalPrice),
                              style: const TextStyle(
                                color: Colors.white54,
                                decoration: TextDecoration.lineThrough,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(width: 12),
                    FilledButton(
                      onPressed: onAdd,
                      style: FilledButton.styleFrom(
                        backgroundColor: const Color(0xFF1FE4FF),
                        foregroundColor: const Color(0xFF08101D),
                      ),
                      child: const Text('加入'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _formatPrice(double value) => '\$${value.toStringAsFixed(2)}';
}

class _ProductImage extends StatelessWidget {
  const _ProductImage({required this.imageUrl});

  final String? imageUrl;

  @override
  Widget build(BuildContext context) {
    final url = imageUrl?.trim();
    if (url == null || url.isEmpty) {
      return Container(
        color: const Color(0xFF08101D),
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
          color: const Color(0xFF08101D),
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
