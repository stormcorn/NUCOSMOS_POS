import 'package:flutter/material.dart';

import '../state/session_controller.dart';
import '../widgets/product_grid.dart';

class PosHomeScreen extends StatefulWidget {
  const PosHomeScreen({
    required this.controller,
    super.key,
  });

  final SessionController controller;

  @override
  State<PosHomeScreen> createState() => _PosHomeScreenState();
}

class _PosHomeScreenState extends State<PosHomeScreen> {
  Future<void> _checkoutCash() async {
    final receipt = await widget.controller.checkoutCash();
    if (!mounted || receipt == null) {
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          '訂單 ${receipt.orderNumber} 已完成，收款 ${_currency(receipt.paidAmount)}',
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.controller,
      builder: (context, _) {
        final controller = widget.controller;

        return Scaffold(
          body: SafeArea(
            child: LayoutBuilder(
              builder: (context, constraints) {
                final wideLayout = constraints.maxWidth >= 1120;

                if (!wideLayout) {
                  return Column(
                    children: [
                      _TopBar(controller: controller),
                      Expanded(
                        child: ListView(
                          padding: const EdgeInsets.all(20),
                          children: [
                            _StoreHeader(controller: controller),
                            const SizedBox(height: 20),
                            _CategoryStrip(controller: controller),
                            const SizedBox(height: 20),
                            SizedBox(
                              height: 520,
                              child: ProductGrid(
                                products: controller.filteredProducts,
                                onAddProduct: controller.addProduct,
                              ),
                            ),
                            const SizedBox(height: 20),
                            _OrderPanel(
                              controller: controller,
                              onCheckoutCash: _checkoutCash,
                            ),
                          ],
                        ),
                      ),
                    ],
                  );
                }

                return Row(
                  children: [
                    SizedBox(
                      width: 280,
                      child: _SidePanel(controller: controller),
                    ),
                    const VerticalDivider(width: 1),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.all(24),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            _StoreHeader(controller: controller),
                            const SizedBox(height: 20),
                            _CategoryStrip(controller: controller),
                            const SizedBox(height: 20),
                            Expanded(
                              child: ProductGrid(
                                products: controller.filteredProducts,
                                onAddProduct: controller.addProduct,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const VerticalDivider(width: 1),
                    SizedBox(
                      width: 360,
                      child: _OrderPanel(
                        controller: controller,
                        onCheckoutCash: _checkoutCash,
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
        );
      },
    );
  }
}

class _TopBar extends StatelessWidget {
  const _TopBar({required this.controller});

  final SessionController controller;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        color: Color(0xFF0A111E),
        border: Border(
          bottom: BorderSide(color: Color(0xFF22314B)),
        ),
      ),
      child: Row(
        children: [
          const Text(
            'NUCOSMOS POS',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: Color(0xFF1FE4FF),
            ),
          ),
          const Spacer(),
          TextButton.icon(
            onPressed: controller.logout,
            icon: const Icon(Icons.logout_rounded),
            label: const Text('登出'),
          ),
        ],
      ),
    );
  }
}

class _SidePanel extends StatelessWidget {
  const _SidePanel({required this.controller});

  final SessionController controller;

  @override
  Widget build(BuildContext context) {
    final session = controller.session;
    final theme = Theme.of(context);

    return Container(
      color: const Color(0xFF0A111E),
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'NUCOSMOS POS',
            style: theme.textTheme.headlineSmall?.copyWith(
              color: const Color(0xFF1FE4FF),
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 24),
          _InfoCard(
            title: '門市',
            value: session?.storeCode ?? '-',
            subtitle: '目前登入門市',
          ),
          const SizedBox(height: 12),
          _InfoCard(
            title: '員工',
            value: session?.displayName ?? '-',
            subtitle: session?.employeeCode ?? '-',
          ),
          const SizedBox(height: 12),
          _InfoCard(
            title: '角色',
            value: session?.activeRole ?? '-',
            subtitle: 'Device: ${session?.deviceCode ?? controller.deviceCode}',
          ),
          const SizedBox(height: 12),
          _InfoCard(
            title: '購物車',
            value: '${controller.cartItemCount} 件',
            subtitle: '小計 ${_currency(controller.cartSubtotal)}',
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed:
                  controller.catalogLoading ? null : () => controller.loadProducts(),
              icon: controller.catalogLoading
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.refresh_rounded),
              label: Text(controller.catalogLoading ? '載入中...' : '重新載入商品'),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: controller.logout,
              icon: const Icon(Icons.logout_rounded),
              label: const Text('登出'),
            ),
          ),
          const Spacer(),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: const Color(0xFF0E1726),
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: const Color(0xFF22314B)),
            ),
            child: const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '目前 POS 第一版已完成',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                SizedBox(height: 10),
                Text('1. PIN 登入與工作階段還原'),
                Text('2. 商品分類與商品清單'),
                Text('3. 購物車與現金結帳'),
                Text('4. 串接 Spring Boot 訂單 API'),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _StoreHeader extends StatelessWidget {
  const _StoreHeader({required this.controller});

  final SessionController controller;

  @override
  Widget build(BuildContext context) {
    final session = controller.session;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 16,
          runSpacing: 12,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            Text(
              'POS 點餐台',
              style: Theme.of(context)
                  .textTheme
                  .headlineMedium
                  ?.copyWith(fontWeight: FontWeight.bold),
            ),
            _Pill(
              label: session == null
                  ? '未登入'
                  : '${session.storeCode} / ${session.activeRole}',
            ),
            _Pill(label: '購物車 ${controller.cartItemCount} 件'),
          ],
        ),
        const SizedBox(height: 10),
        Text(
          '先從商品分類開始點餐，右側會即時累積當前訂單並可直接現金結帳。',
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                color: Colors.white70,
              ),
        ),
        if (controller.errorMessage.isNotEmpty) ...[
          const SizedBox(height: 12),
          _InlineMessage(
            color: const Color(0xFF3A0E14),
            borderColor: Colors.redAccent,
            message: controller.errorMessage,
          ),
        ],
        if (controller.checkoutMessage.isNotEmpty) ...[
          const SizedBox(height: 12),
          _InlineMessage(
            color: const Color(0xFF0E2B24),
            borderColor: const Color(0xFF2DF3C4),
            message: controller.checkoutMessage,
          ),
        ],
      ],
    );
  }
}

class _CategoryStrip extends StatelessWidget {
  const _CategoryStrip({required this.controller});

  final SessionController controller;

  @override
  Widget build(BuildContext context) {
    final categories = controller.categories;
    return SizedBox(
      height: 52,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: FilterChip(
              label: Text('全部 (${controller.availableProducts.length})'),
              selected: controller.selectedCategoryCode == null,
              onSelected: (_) => controller.selectCategory(null),
            ),
          ),
          ...categories.map(
            (category) => Padding(
              padding: const EdgeInsets.only(right: 12),
              child: FilterChip(
                label: Text('${category.name} (${category.count})'),
                selected: controller.selectedCategoryCode == category.code,
                onSelected: (_) => controller.selectCategory(category.code),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _OrderPanel extends StatelessWidget {
  const _OrderPanel({
    required this.controller,
    required this.onCheckoutCash,
  });

  final SessionController controller;
  final Future<void> Function() onCheckoutCash;

  @override
  Widget build(BuildContext context) {
    final cart = controller.cart;
    final lastOrder = controller.lastCompletedOrder;

    return Container(
      color: const Color(0xFF0A111E),
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '當前訂單',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
          ),
          const SizedBox(height: 8),
          Text(
            '商品數 ${controller.cartItemCount} 件',
            style: const TextStyle(color: Colors.white70),
          ),
          if (lastOrder != null) ...[
            const SizedBox(height: 16),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFF0E2B24),
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: const Color(0xFF2DF3C4)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '最近完成訂單 ${lastOrder.orderNumber}',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '總額 ${_currency(lastOrder.totalAmount)} / 已收 ${_currency(lastOrder.paidAmount)}',
                    style: const TextStyle(color: Colors.white70),
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 16),
          Expanded(
            child: cart.isEmpty
                ? Container(
                    width: double.infinity,
                    decoration: BoxDecoration(
                      color: const Color(0xFF0E1726),
                      borderRadius: BorderRadius.circular(22),
                      border: Border.all(color: const Color(0xFF22314B)),
                    ),
                    alignment: Alignment.center,
                    child: const Text(
                      '尚未加入商品。\n請從左側商品卡點擊「加入」。',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.white60, height: 1.7),
                    ),
                  )
                : ListView.separated(
                    itemCount: cart.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final line = cart[index];
                      return _CartLineTile(
                        line: line,
                        onIncrease: () =>
                            controller.increaseQuantity(line.product.id),
                        onDecrease: () =>
                            controller.decreaseQuantity(line.product.id),
                        onRemove: () => controller.removeProduct(line.product.id),
                      );
                    },
                  ),
          ),
          const SizedBox(height: 16),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: const Color(0xFF0E1726),
              borderRadius: BorderRadius.circular(22),
              border: Border.all(color: const Color(0xFF22314B)),
            ),
            child: Column(
              children: [
                _SummaryRow(label: '品項數', value: '${controller.cartItemCount}'),
                const SizedBox(height: 10),
                _SummaryRow(
                  label: '小計',
                  value: _currency(controller.cartSubtotal),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: cart.isEmpty || controller.checkoutLoading
                            ? null
                            : controller.clearCart,
                        child: const Text('清空'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: FilledButton.icon(
                        onPressed: cart.isEmpty || controller.checkoutLoading
                            ? null
                            : onCheckoutCash,
                        icon: controller.checkoutLoading
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(strokeWidth: 2),
                              )
                            : const Icon(Icons.payments_rounded),
                        label: Text(
                          controller.checkoutLoading ? '結帳中...' : '現金結帳',
                        ),
                      ),
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
}

class _CartLineTile extends StatelessWidget {
  const _CartLineTile({
    required this.line,
    required this.onIncrease,
    required this.onDecrease,
    required this.onRemove,
  });

  final PosCartLine line;
  final VoidCallback onIncrease;
  final VoidCallback onDecrease;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFF0E1726),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFF22314B)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      line.product.name,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      line.product.categoryName,
                      style: const TextStyle(color: Colors.white60),
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: onRemove,
                icon: const Icon(Icons.close_rounded),
                tooltip: '移除',
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              _QuantityButton(
                icon: Icons.remove_rounded,
                onTap: onDecrease,
              ),
              Container(
                width: 56,
                alignment: Alignment.center,
                child: Text(
                  '${line.quantity}',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              _QuantityButton(
                icon: Icons.add_rounded,
                onTap: onIncrease,
              ),
              const Spacer(),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    '${_currency(line.product.price)} x ${line.quantity}',
                    style: const TextStyle(color: Colors.white60),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _currency(line.lineTotal),
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF1FE4FF),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _QuantityButton extends StatelessWidget {
  const _QuantityButton({
    required this.icon,
    required this.onTap,
  });

  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Ink(
        width: 42,
        height: 42,
        decoration: BoxDecoration(
          color: const Color(0xFF08101D),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: const Color(0xFF22314B)),
        ),
        child: Icon(icon, size: 18),
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Text(label, style: const TextStyle(color: Colors.white70)),
        const Spacer(),
        Text(
          value,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }
}

class _InlineMessage extends StatelessWidget {
  const _InlineMessage({
    required this.color,
    required this.borderColor,
    required this.message,
  });

  final Color color;
  final Color borderColor;
  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: borderColor),
      ),
      child: Text(message),
    );
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFF0E1726),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: const Color(0xFF263652)),
      ),
      child: Text(label),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({
    required this.title,
    required this.value,
    required this.subtitle,
  });

  final String title;
  final String value;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF0E1726),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFF22314B)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(color: Colors.white70)),
          const SizedBox(height: 8),
          Text(
            value,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 4),
          Text(subtitle, style: const TextStyle(color: Colors.white60)),
        ],
      ),
    );
  }
}

String _currency(double value) => '\$${value.toStringAsFixed(2)}';
