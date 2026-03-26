import 'package:flutter/material.dart';
import 'package:flutter_thermal_printer/utils/printer.dart';

import '../models/product_summary.dart';
import '../models/quick_receive_models.dart';
import '../state/printer_controller.dart';
import '../state/session_controller.dart';
import '../widgets/adaptive_scroll_body.dart';
import '../widgets/product_grid.dart';

enum _PosWorkspace { sales, quickReceive }

class PosHomeScreen extends StatefulWidget {
  const PosHomeScreen({
    required this.controller,
    required this.printerController,
    super.key,
  });

  final SessionController controller;
  final PrinterController printerController;

  @override
  State<PosHomeScreen> createState() => _PosHomeScreenState();
}

class _PosHomeScreenState extends State<PosHomeScreen> {
  _PosWorkspace _workspace = _PosWorkspace.sales;
  QuickReceiveItemType _quickReceiveType = QuickReceiveItemType.material;
  String _quickReceiveSearch = '';
  QuickReceiveItem? _selectedReceiveItem;

  Future<void> _handleAddProduct(ProductSummary product) async {
    if (product.customizationGroups.isEmpty) {
      widget.controller.addProduct(product);
      return;
    }

    final selections = await _openCustomizationSheet(product);
    if (!mounted || selections == null) {
      return;
    }

    widget.controller.addProduct(product, selectedOptions: selections);
  }

  Future<void> _openQuickReceiveWorkspace() async {
    if (!widget.controller.canUseQuickReceive) {
      return;
    }
    setState(() {
      _workspace = _PosWorkspace.quickReceive;
    });
    await widget.controller.loadQuickReceiveCatalog();
    if (!mounted) {
      return;
    }
    setState(() {
      _selectedReceiveItem ??=
          widget.controller.itemsForReceiveType(_quickReceiveType).isNotEmpty
              ? widget.controller.itemsForReceiveType(_quickReceiveType).first
              : null;
    });
  }

  void _openSalesWorkspace() {
    setState(() {
      _workspace = _PosWorkspace.sales;
    });
  }

  void _setQuickReceiveType(QuickReceiveItemType type) {
    setState(() {
      _quickReceiveType = type;
      _selectedReceiveItem = null;
    });
  }

  Future<void> _submitQuickReceive({
    required QuickReceiveItem item,
    required int purchaseQuantity,
    double? purchaseUnitCost,
    String? note,
  }) async {
    final result = await widget.controller.submitQuickReceive(
      item: item,
      purchaseQuantity: purchaseQuantity,
      purchaseUnitCost: purchaseUnitCost,
      note: note,
    );
    if (!mounted || result == null) {
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          '${item.name} 已入庫 ${result.receivedStockQuantity} ${item.unit}，目前庫存 ${result.quantityAfter} ${item.unit}',
        ),
      ),
    );
  }

  Future<void> _createQuickReceiveItem({
    required QuickReceiveItemType type,
    required String sku,
    required String name,
    required String unit,
    required String purchaseUnit,
    required int purchaseToStockRatio,
    required int reorderLevel,
    String? description,
    double? latestUnitCost,
  }) async {
    final createdItem = await widget.controller.createQuickReceiveItem(
      type: type,
      sku: sku,
      name: name,
      unit: unit,
      purchaseUnit: purchaseUnit,
      purchaseToStockRatio: purchaseToStockRatio,
      reorderLevel: reorderLevel,
      description: description,
      latestUnitCost: latestUnitCost,
    );
    if (!mounted || createdItem == null) {
      return;
    }

    setState(() {
      _quickReceiveType = createdItem.type;
      _quickReceiveSearch = '';
      _selectedReceiveItem = createdItem;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('${createdItem.type.label} ${createdItem.name} 已建立'),
      ),
    );
  }

  Future<void> _checkoutCash() async {
    final cartSnapshot = widget.controller.cart.toList(growable: false);
    final receipt = await widget.controller.checkoutCash();
    if (!mounted || receipt == null) {
      return;
    }

    await widget.printerController.printReceiptForOrder(
      receipt: receipt,
      lines: cartSnapshot,
      storeCode: widget.controller.session?.storeCode,
      staffName: widget.controller.session?.displayName,
    );

    if (!mounted) {
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Order ${receipt.orderNumber} completed. Paid ${_currency(receipt.paidAmount)}',
        ),
      ),
    );
  }

  void _openPrinterSheet() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF111827),
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: SingleChildScrollView(
              child: _PrinterPanel(printerController: widget.printerController),
            ),
          ),
        );
      },
    );
  }

  Future<List<PosCartSelection>?> _openCustomizationSheet(
    ProductSummary product,
  ) {
    return showModalBottomSheet<List<PosCartSelection>>(
      context: context,
      isScrollControlled: true,
      backgroundColor: const Color(0xFF101827),
      builder: (context) => _CustomizationSheet(product: product),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: Listenable.merge([
        widget.controller,
        widget.printerController,
      ]),
      builder: (context, _) {
        final controller = widget.controller;
        final selectedCategoryName = _selectedCategoryName(controller);
        final screenWidth = MediaQuery.of(context).size.width;
        final wideLayout = screenWidth >= 940;
        final desktopLayout = screenWidth >= 1220;
        final contentPadding = desktopLayout ? 20.0 : 14.0;
        final cartWidth = desktopLayout ? 310.0 : 272.0;
        final categoryWidth = desktopLayout ? 148.0 : 126.0;
        final receiveItems =
            controller.itemsForReceiveType(_quickReceiveType).where((item) {
          final keyword = _quickReceiveSearch.trim().toLowerCase();
          if (keyword.isEmpty) {
            return true;
          }
          return item.name.toLowerCase().contains(keyword) ||
              item.sku.toLowerCase().contains(keyword);
        }).toList(growable: false);
        final selectedReceiveItem = receiveItems.any(
          (item) => item.id == _selectedReceiveItem?.id,
        )
            ? receiveItems
                .firstWhere((item) => item.id == _selectedReceiveItem?.id)
            : (receiveItems.isNotEmpty ? receiveItems.first : null);

        return Scaffold(
          body: SafeArea(
            child: Row(
              children: [
                _NavigationRail(
                  salesActive: _workspace == _PosWorkspace.sales,
                  receiveActive: _workspace == _PosWorkspace.quickReceive,
                  showQuickReceive: controller.canUseQuickReceive,
                  onOpenSales: _openSalesWorkspace,
                  onOpenQuickReceive: _openQuickReceiveWorkspace,
                  onOpenPrinter: _openPrinterSheet,
                  onLogout: controller.logout,
                  onRefresh: () => controller.loadProducts(),
                ),
                Expanded(
                  child: Container(
                    color: const Color(0xFF171F2E),
                    child: _workspace == _PosWorkspace.quickReceive
                        ? _QuickReceiveWorkspace(
                            controller: controller,
                            items: receiveItems,
                            selectedItem: selectedReceiveItem,
                            receiveType: _quickReceiveType,
                            wideLayout: wideLayout,
                            desktopLayout: desktopLayout,
                            contentPadding: contentPadding,
                            searchValue: _quickReceiveSearch,
                            onSearchChanged: (value) {
                              setState(() {
                                _quickReceiveSearch = value;
                              });
                            },
                            onTypeChanged: _setQuickReceiveType,
                            onItemSelected: (item) {
                              setState(() {
                                _selectedReceiveItem = item;
                              });
                            },
                            onCreateItem: _createQuickReceiveItem,
                            onSubmit: _submitQuickReceive,
                          )
                        : wideLayout
                            ? Row(
                                children: [
                                  SizedBox(
                                    width: categoryWidth,
                                    child: _CategorySidebar(
                                        controller: controller),
                                  ),
                                  Expanded(
                                    child: Padding(
                                      padding: EdgeInsets.fromLTRB(
                                        contentPadding,
                                        16,
                                        contentPadding,
                                        16,
                                      ),
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          _HeaderSection(
                                            title: selectedCategoryName,
                                            subtitle:
                                                'Select your favorite beverages',
                                            sessionController: controller,
                                            compact: !desktopLayout,
                                          ),
                                          SizedBox(
                                            height: desktopLayout ? 16 : 12,
                                          ),
                                          Expanded(
                                            child: ProductGrid(
                                              products:
                                                  controller.filteredProducts,
                                              onAddProduct: _handleAddProduct,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ),
                                  SizedBox(
                                    width: cartWidth,
                                    child: _CurrentOrderPanel(
                                      controller: controller,
                                      onCheckout: _checkoutCash,
                                      compact: !desktopLayout,
                                    ),
                                  ),
                                ],
                              )
                            : Column(
                                children: [
                                  Expanded(
                                    child: ListView(
                                      padding: const EdgeInsets.fromLTRB(
                                        18,
                                        20,
                                        18,
                                        24,
                                      ),
                                      children: [
                                        _HeaderSection(
                                          title: selectedCategoryName,
                                          subtitle:
                                              'Select your favorite beverages',
                                          sessionController: controller,
                                          compact: true,
                                        ),
                                        const SizedBox(height: 18),
                                        _CategoryChips(controller: controller),
                                        const SizedBox(height: 18),
                                        ProductGrid(
                                          products: controller.filteredProducts,
                                          onAddProduct: _handleAddProduct,
                                          embedInParentScroll: true,
                                        ),
                                        const SizedBox(height: 18),
                                        _CurrentOrderPanel(
                                          controller: controller,
                                          onCheckout: _checkoutCash,
                                          compact: true,
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  String _selectedCategoryName(SessionController controller) {
    final selectedCode = controller.selectedCategoryCode;
    if (selectedCode == null || selectedCode.isEmpty) {
      return 'Tea & Drinks';
    }

    final match =
        controller.categories.where((item) => item.code == selectedCode);
    if (match.isEmpty) {
      return 'Tea & Drinks';
    }

    return match.first.name;
  }
}

class _NavigationRail extends StatelessWidget {
  const _NavigationRail({
    required this.salesActive,
    required this.receiveActive,
    required this.showQuickReceive,
    required this.onOpenSales,
    required this.onOpenQuickReceive,
    required this.onOpenPrinter,
    required this.onLogout,
    required this.onRefresh,
  });

  final bool salesActive;
  final bool receiveActive;
  final bool showQuickReceive;
  final VoidCallback onOpenSales;
  final VoidCallback onOpenQuickReceive;
  final VoidCallback onOpenPrinter;
  final VoidCallback onLogout;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 72,
      color: const Color(0xFF0A0F17),
      child: Column(
        children: [
          const SizedBox(height: 12),
          Container(
            width: 34,
            height: 34,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(10),
              gradient: const LinearGradient(
                colors: [Color(0xFF14F1FF), Color(0xFF0FE7A7)],
              ),
            ),
            child: const Text(
              'N',
              style: TextStyle(
                color: Color(0xFF07111C),
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          const SizedBox(height: 26),
          _RailButton(
            icon: Icons.local_cafe_rounded,
            active: salesActive,
            onTap: onOpenSales,
          ),
          if (showQuickReceive)
            _RailButton(
              icon: Icons.inventory_2_rounded,
              active: receiveActive,
              onTap: onOpenQuickReceive,
            ),
          _RailButton(
            icon: Icons.grid_view_rounded,
            onTap: onRefresh,
          ),
          _RailButton(
            icon: Icons.print_rounded,
            onTap: onOpenPrinter,
          ),
          const Spacer(),
          _RailButton(
            icon: Icons.logout_rounded,
            onTap: onLogout,
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }
}

class _RailButton extends StatelessWidget {
  const _RailButton({
    required this.icon,
    required this.onTap,
    this.active = false,
  });

  final IconData icon;
  final VoidCallback onTap;
  final bool active;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Ink(
          width: 42,
          height: 42,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(14),
            color: active ? const Color(0xFF111E2F) : const Color(0xFF1A2434),
            border: Border.all(
              color: active ? const Color(0xFF14F1FF) : Colors.transparent,
            ),
          ),
          child: Icon(
            icon,
            color: active ? const Color(0xFF14F1FF) : Colors.white70,
            size: 20,
          ),
        ),
      ),
    );
  }
}

class _HeaderSection extends StatelessWidget {
  const _HeaderSection({
    required this.title,
    required this.subtitle,
    required this.sessionController,
    this.compact = false,
  });

  final String title;
  final String subtitle;
  final SessionController sessionController;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: TextStyle(
            fontSize: compact ? 18 : 22,
            fontWeight: FontWeight.w800,
            color: const Color(0xFF14F1FF),
          ),
        ),
        SizedBox(height: compact ? 4 : 6),
        Text(
          subtitle,
          style: TextStyle(
            color: const Color(0xFF97A7BC),
            fontSize: compact ? 12 : 15,
          ),
        ),
        if (sessionController.errorMessage.isNotEmpty) ...[
          const SizedBox(height: 12),
          _BannerMessage(
            color: const Color(0xFF35161B),
            borderColor: const Color(0xFFB74B57),
            message: sessionController.errorMessage,
          ),
        ],
        if (sessionController.checkoutMessage.isNotEmpty) ...[
          const SizedBox(height: 12),
          _BannerMessage(
            color: const Color(0xFF122A27),
            borderColor: const Color(0xFF14F1FF),
            message: sessionController.checkoutMessage,
          ),
        ],
      ],
    );
  }
}

class _CategorySidebar extends StatelessWidget {
  const _CategorySidebar({required this.controller});

  final SessionController controller;

  @override
  Widget build(BuildContext context) {
    final categories = [
      const _SidebarCategoryItem(
        code: null,
        label: 'All Items',
        icon: Icons.grid_view_rounded,
      ),
      ...controller.categories.map(
        (item) => _SidebarCategoryItem(
          code: item.code,
          label: item.name,
          icon: Icons.local_cafe_rounded,
        ),
      ),
    ];

    return Container(
      color: const Color(0xFF121927),
      padding: const EdgeInsets.fromLTRB(10, 16, 10, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 6),
            child: Text(
              'Categories',
              style: TextStyle(
                color: Color(0xFF14F1FF),
                fontSize: 13,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          const SizedBox(height: 12),
          Expanded(
            child: ListView.separated(
              itemCount: categories.length,
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, index) {
                final item = categories[index];
                final active = controller.selectedCategoryCode == item.code ||
                    (item.code == null &&
                        controller.selectedCategoryCode == null);
                return InkWell(
                  onTap: () => controller.selectCategory(item.code),
                  borderRadius: BorderRadius.circular(16),
                  child: Ink(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10,
                      vertical: 12,
                    ),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(16),
                      color: active
                          ? const Color(0xFF14F1FF)
                          : const Color(0xFF1E293B),
                      border: Border.all(
                        color: active
                            ? const Color(0xFF14F1FF)
                            : const Color(0xFF29364A),
                      ),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          item.icon,
                          size: 16,
                          color:
                              active ? const Color(0xFF07111C) : Colors.white70,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            item.label,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: active
                                  ? const Color(0xFF07111C)
                                  : Colors.white,
                              fontSize: 12.5,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _SidebarCategoryItem {
  const _SidebarCategoryItem({
    required this.code,
    required this.label,
    required this.icon,
  });

  final String? code;
  final String label;
  final IconData icon;
}

class _CategoryChips extends StatelessWidget {
  const _CategoryChips({required this.controller});

  final SessionController controller;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 42,
      child: ListView(
        scrollDirection: Axis.horizontal,
        children: [
          _CategoryChip(
            label: 'All',
            active: controller.selectedCategoryCode == null,
            onTap: () => controller.selectCategory(null),
          ),
          const SizedBox(width: 10),
          ...controller.categories.expand(
            (item) => [
              _CategoryChip(
                label: item.name,
                active: controller.selectedCategoryCode == item.code,
                onTap: () => controller.selectCategory(item.code),
              ),
              const SizedBox(width: 10),
            ],
          ),
        ],
      ),
    );
  }
}

class _CategoryChip extends StatelessWidget {
  const _CategoryChip({
    required this.label,
    required this.active,
    required this.onTap,
  });

  final String label;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(999),
      child: Ink(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(999),
          color: active ? const Color(0xFF14F1FF) : const Color(0xFF243047),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: active ? const Color(0xFF07111C) : Colors.white,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
    );
  }
}

class _QuickReceiveWorkspace extends StatelessWidget {
  const _QuickReceiveWorkspace({
    required this.controller,
    required this.items,
    required this.selectedItem,
    required this.receiveType,
    required this.wideLayout,
    required this.desktopLayout,
    required this.contentPadding,
    required this.searchValue,
    required this.onSearchChanged,
    required this.onTypeChanged,
    required this.onItemSelected,
    required this.onCreateItem,
    required this.onSubmit,
  });

  final SessionController controller;
  final List<QuickReceiveItem> items;
  final QuickReceiveItem? selectedItem;
  final QuickReceiveItemType receiveType;
  final bool wideLayout;
  final bool desktopLayout;
  final double contentPadding;
  final String searchValue;
  final ValueChanged<String> onSearchChanged;
  final ValueChanged<QuickReceiveItemType> onTypeChanged;
  final ValueChanged<QuickReceiveItem> onItemSelected;
  final Future<void> Function({
    required QuickReceiveItemType type,
    required String sku,
    required String name,
    required String unit,
    required String purchaseUnit,
    required int purchaseToStockRatio,
    required int reorderLevel,
    String? description,
    double? latestUnitCost,
  }) onCreateItem;
  final Future<void> Function({
    required QuickReceiveItem item,
    required int purchaseQuantity,
    double? purchaseUnitCost,
    String? note,
  }) onSubmit;

  @override
  Widget build(BuildContext context) {
    final header = _HeaderSection(
      title: '快速收貨',
      subtitle: '原料與包裝現場入庫',
      sessionController: controller,
      compact: !desktopLayout,
    );

    final listPane = _QuickReceiveListPane(
      controller: controller,
      items: items,
      selectedItem: selectedItem,
      receiveType: receiveType,
      searchValue: searchValue,
      onSearchChanged: onSearchChanged,
      onTypeChanged: onTypeChanged,
      onItemSelected: onItemSelected,
    );

    final formPane = _QuickReceiveFormPane(
      controller: controller,
      item: selectedItem,
      itemType: receiveType,
      embedInParentScroll: !wideLayout,
      onCreateItem: onCreateItem,
      onSubmit: onSubmit,
    );

    if (wideLayout) {
      return Padding(
        padding: EdgeInsets.fromLTRB(contentPadding, 16, contentPadding, 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            header,
            SizedBox(height: desktopLayout ? 16 : 12),
            Expanded(
              child: Row(
                children: [
                  SizedBox(
                    width: desktopLayout ? 390 : 342,
                    child: listPane,
                  ),
                  const SizedBox(width: 16),
                  Expanded(child: formPane),
                ],
              ),
            ),
          ],
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 20, 18, 24),
      children: [
        header,
        const SizedBox(height: 18),
        SizedBox(height: 420, child: listPane),
        const SizedBox(height: 18),
        formPane,
      ],
    );
  }
}

class _QuickReceiveListPane extends StatelessWidget {
  const _QuickReceiveListPane({
    required this.controller,
    required this.items,
    required this.selectedItem,
    required this.receiveType,
    required this.searchValue,
    required this.onSearchChanged,
    required this.onTypeChanged,
    required this.onItemSelected,
  });

  final SessionController controller;
  final List<QuickReceiveItem> items;
  final QuickReceiveItem? selectedItem;
  final QuickReceiveItemType receiveType;
  final String searchValue;
  final ValueChanged<String> onSearchChanged;
  final ValueChanged<QuickReceiveItemType> onTypeChanged;
  final ValueChanged<QuickReceiveItem> onItemSelected;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF121927),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFF253043)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '收貨清單',
              style: TextStyle(
                color: Color(0xFF14F1FF),
                fontSize: 15,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _TypeToggleChip(
                    label: QuickReceiveItemType.material.label,
                    active: receiveType == QuickReceiveItemType.material,
                    onTap: () => onTypeChanged(QuickReceiveItemType.material),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _TypeToggleChip(
                    label: QuickReceiveItemType.manufactured.label,
                    active: receiveType == QuickReceiveItemType.manufactured,
                    onTap: () =>
                        onTypeChanged(QuickReceiveItemType.manufactured),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _TypeToggleChip(
                    label: QuickReceiveItemType.packaging.label,
                    active: receiveType == QuickReceiveItemType.packaging,
                    onTap: () => onTypeChanged(QuickReceiveItemType.packaging),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextFormField(
              onChanged: onSearchChanged,
              initialValue: searchValue,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                hintText: '搜尋 SKU 或品項',
                hintStyle: const TextStyle(color: Colors.white38),
                prefixIcon:
                    const Icon(Icons.search_rounded, color: Colors.white54),
                filled: true,
                fillColor: const Color(0xFF1A2332),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
            const SizedBox(height: 12),
            Expanded(
              child: controller.quickReceiveLoading
                  ? const Center(child: CircularProgressIndicator())
                  : items.isEmpty
                      ? const Center(
                          child: Text(
                            '目前沒有可收貨品項',
                            style: TextStyle(color: Colors.white54),
                          ),
                        )
                      : ListView.separated(
                          itemCount: items.length,
                          separatorBuilder: (_, __) =>
                              const SizedBox(height: 10),
                          itemBuilder: (context, index) {
                            final item = items[index];
                            final active = item.id == selectedItem?.id;
                            return InkWell(
                              onTap: () => onItemSelected(item),
                              borderRadius: BorderRadius.circular(18),
                              child: Ink(
                                padding: const EdgeInsets.all(14),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(18),
                                  color: active
                                      ? const Color(0xFF223047)
                                      : const Color(0xFF172132),
                                  border: Border.all(
                                    color: active
                                        ? const Color(0xFF14F1FF)
                                        : const Color(0xFF253043),
                                  ),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      children: [
                                        Expanded(
                                          child: Text(
                                            item.name,
                                            style: const TextStyle(
                                              color: Colors.white,
                                              fontSize: 15,
                                              fontWeight: FontWeight.w700,
                                            ),
                                          ),
                                        ),
                                        if (item.lowStock)
                                          Container(
                                            padding: const EdgeInsets.symmetric(
                                              horizontal: 8,
                                              vertical: 4,
                                            ),
                                            decoration: BoxDecoration(
                                              color: const Color(0xFF35161B),
                                              borderRadius:
                                                  BorderRadius.circular(999),
                                            ),
                                            child: const Text(
                                              '低庫存',
                                              style: TextStyle(
                                                color: Color(0xFFFF8A93),
                                                fontSize: 11,
                                                fontWeight: FontWeight.w700,
                                              ),
                                            ),
                                          ),
                                      ],
                                    ),
                                    const SizedBox(height: 6),
                                    Text(
                                      '${item.sku} · 現有 ${item.quantityOnHand} ${item.unit}',
                                      style: const TextStyle(
                                        color: Color(0xFF8DA2BD),
                                        fontSize: 12,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      item.subtitle,
                                      maxLines: 2,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                        color: Colors.white60,
                                        fontSize: 12,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
            ),
          ],
        ),
      ),
    );
  }
}

class _QuickReceiveFormPane extends StatefulWidget {
  const _QuickReceiveFormPane({
    required this.controller,
    required this.item,
    required this.itemType,
    required this.embedInParentScroll,
    required this.onCreateItem,
    required this.onSubmit,
  });

  final SessionController controller;
  final QuickReceiveItem? item;
  final QuickReceiveItemType itemType;
  final bool embedInParentScroll;
  final Future<void> Function({
    required QuickReceiveItemType type,
    required String sku,
    required String name,
    required String unit,
    required String purchaseUnit,
    required int purchaseToStockRatio,
    required int reorderLevel,
    String? description,
    double? latestUnitCost,
  }) onCreateItem;
  final Future<void> Function({
    required QuickReceiveItem item,
    required int purchaseQuantity,
    double? purchaseUnitCost,
    String? note,
  }) onSubmit;

  @override
  State<_QuickReceiveFormPane> createState() => _QuickReceiveFormPaneState();
}

class _QuickReceiveFormPaneState extends State<_QuickReceiveFormPane> {
  final TextEditingController _quantityController =
      TextEditingController(text: '1');
  final TextEditingController _costController = TextEditingController();
  final TextEditingController _noteController = TextEditingController();
  final TextEditingController _createSkuController = TextEditingController();
  final TextEditingController _createNameController = TextEditingController();
  final TextEditingController _createUnitController = TextEditingController();
  final TextEditingController _createPurchaseUnitController =
      TextEditingController();
  final TextEditingController _createRatioController =
      TextEditingController(text: '1');
  final TextEditingController _createReorderLevelController =
      TextEditingController(text: '0');
  final TextEditingController _createDescriptionController =
      TextEditingController();
  final TextEditingController _createCostController = TextEditingController();
  String _localError = '';

  @override
  void didUpdateWidget(covariant _QuickReceiveFormPane oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.item?.id != widget.item?.id) {
      _quantityController.text = '1';
      _costController.text =
          widget.item?.latestPurchaseUnitCost?.toStringAsFixed(2) ?? '';
      _noteController.clear();
      _localError = '';
    }
    if (oldWidget.itemType != widget.itemType) {
      _clearCreateForm();
    }
  }

  @override
  void dispose() {
    _quantityController.dispose();
    _costController.dispose();
    _noteController.dispose();
    _createSkuController.dispose();
    _createNameController.dispose();
    _createUnitController.dispose();
    _createPurchaseUnitController.dispose();
    _createRatioController.dispose();
    _createReorderLevelController.dispose();
    _createDescriptionController.dispose();
    _createCostController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final item = widget.item;
    final useEmbeddedLayout = widget.embedInParentScroll;
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF0C1118),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFF253043)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: item == null
            ? _buildEmptyState(useEmbeddedLayout)
            : Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.name,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 24,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 8),
                  if (useEmbeddedLayout)
                    AdaptiveScrollBody(
                      embedInParentScroll: true,
                      children: _buildSelectedItemScrollableContent(item),
                    )
                  else
                    Expanded(
                      child: AdaptiveScrollBody(
                        embedInParentScroll: false,
                        children: _buildSelectedItemScrollableContent(item),
                      ),
                    ),
                ],
              ),
      ),
    );
  }

  List<Widget> _buildSelectedItemScrollableContent(QuickReceiveItem item) {
    return [
      Wrap(
        spacing: 10,
        runSpacing: 10,
        children: [
          _InfoPill(label: '類型', value: item.type.label),
          _InfoPill(label: 'SKU', value: item.sku),
          _InfoPill(
              label: '現有庫存', value: '${item.quantityOnHand} ${item.unit}'),
          _InfoPill(
            label: '換算',
            value:
                '1 ${item.purchaseUnit} = ${item.purchaseToStockRatio} ${item.unit}',
          ),
        ],
      ),
      const SizedBox(height: 18),
      if (_localError.isNotEmpty)
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: _BannerMessage(
            color: const Color(0xFF35161B),
            borderColor: const Color(0xFFB74B57),
            message: _localError,
          ),
        ),
      if (widget.controller.quickReceiveMessage.isNotEmpty)
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: _BannerMessage(
            color: const Color(0xFF122A27),
            borderColor: const Color(0xFF14F1FF),
            message: widget.controller.quickReceiveMessage,
          ),
        ),
      const SizedBox(height: 12),
      _buildCreateSection(),
      const SizedBox(height: 18),
      ..._buildReceiveFormFields(item),
      const SizedBox(height: 16),
      SizedBox(
        width: double.infinity,
        height: 52,
        child: FilledButton.icon(
          onPressed: widget.controller.quickReceiveSaving
              ? null
              : () => _handleSubmit(item),
          icon: widget.controller.quickReceiveSaving
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.move_to_inbox_rounded),
          label: Text(
            widget.controller.quickReceiveSaving ? '處理中...' : '確認收貨',
            style: const TextStyle(fontWeight: FontWeight.w800),
          ),
        ),
      ),
    ];
  }

  Widget _buildEmptyState(bool useEmbeddedLayout) {
    const emptyHint = Center(
      child: Text(
        '請先從左側選擇品項，或直接建立新品項後再收貨。',
        style: TextStyle(color: Colors.white54),
        textAlign: TextAlign.center,
      ),
    );

    return AdaptiveScrollBody(
      embedInParentScroll: useEmbeddedLayout,
      children: [
        _buildCreateSection(),
        const SizedBox(height: 16),
        const Padding(
          padding: EdgeInsets.symmetric(vertical: 12),
          child: emptyHint,
        ),
      ],
    );
  }

  List<Widget> _buildReceiveFormFields(QuickReceiveItem item) {
    return [
      _ReceiveField(
        label: '收貨數量',
        child: TextField(
          controller: _quantityController,
          keyboardType: TextInputType.number,
          style: const TextStyle(color: Colors.white),
          decoration: _inputDecoration('輸入 ${item.purchaseUnit} 數量'),
        ),
      ),
      const SizedBox(height: 14),
      _ReceiveField(
        label: '最近採購成本',
        helper: '若有需要，可更新這次的每 ${item.unit} 採購成本',
        child: TextField(
          controller: _costController,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          style: const TextStyle(color: Colors.white),
          decoration: _inputDecoration('例如 320.00'),
        ),
      ),
      const SizedBox(height: 14),
      _ReceiveField(
        label: '備註',
        child: TextField(
          controller: _noteController,
          maxLines: 3,
          style: const TextStyle(color: Colors.white),
          decoration: _inputDecoration('例如 供應商到貨、臨時補貨等'),
        ),
      ),
      const SizedBox(height: 18),
      Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFF151D2C),
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: const Color(0xFF253043)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '入庫預覽',
              style: TextStyle(
                color: Color(0xFF14F1FF),
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 10),
            Text(
              '本次將以 PURCHASE_IN 增加入庫 ${_previewStockQuantity(item)} ${item.unit}。',
              style: const TextStyle(color: Colors.white70),
            ),
          ],
        ),
      ),
    ];
  }

  int _previewStockQuantity(QuickReceiveItem item) {
    final quantity = int.tryParse(_quantityController.text.trim()) ?? 1;
    return quantity * item.purchaseToStockRatio;
  }

  Widget _buildCreateSection() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF151D2C),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFF253043)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '建立${widget.itemType.label}',
            style: const TextStyle(
              color: Color(0xFF14F1FF),
              fontWeight: FontWeight.w800,
              fontSize: 16,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            '現場若沒有既有品項，可以先建立基本資料，再直接完成收貨。',
            style: TextStyle(color: Colors.white60, fontSize: 12),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _createSkuController,
                  style: const TextStyle(color: Colors.white),
                  decoration: _inputDecoration('SKU'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextField(
                  controller: _createNameController,
                  style: const TextStyle(color: Colors.white),
                  decoration: _inputDecoration('名稱'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _createUnitController,
                  style: const TextStyle(color: Colors.white),
                  decoration: _inputDecoration('庫存單位'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextField(
                  controller: _createPurchaseUnitController,
                  style: const TextStyle(color: Colors.white),
                  decoration: _inputDecoration('採購單位'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _createRatioController,
                  keyboardType: TextInputType.number,
                  style: const TextStyle(color: Colors.white),
                  decoration: _inputDecoration('換算比例'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextField(
                  controller: _createReorderLevelController,
                  keyboardType: TextInputType.number,
                  style: const TextStyle(color: Colors.white),
                  decoration: _inputDecoration('補貨線'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _createCostController,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            style: const TextStyle(color: Colors.white),
            decoration: _inputDecoration('最近採購成本，可留空'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _createDescriptionController,
            maxLines: 2,
            style: const TextStyle(color: Colors.white),
            decoration: _inputDecoration('描述或備註'),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: widget.controller.quickReceiveSaving
                  ? null
                  : _handleCreateItem,
              icon: const Icon(Icons.add_box_rounded),
              label: Text('建立${widget.itemType.label}'),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _handleSubmit(QuickReceiveItem item) async {
    final purchaseQuantity = int.tryParse(_quantityController.text.trim());
    final purchaseUnitCost = _costController.text.trim().isEmpty
        ? null
        : double.tryParse(_costController.text.trim());

    if (purchaseQuantity == null || purchaseQuantity <= 0) {
      setState(() {
        _localError = '請輸入大於 0 的收貨數量。';
      });
      return;
    }

    if (_costController.text.trim().isNotEmpty &&
        (purchaseUnitCost == null || purchaseUnitCost < 0)) {
      setState(() {
        _localError = '請輸入有效的採購成本。';
      });
      return;
    }

    setState(() {
      _localError = '';
    });

    await widget.onSubmit(
      item: item,
      purchaseQuantity: purchaseQuantity,
      purchaseUnitCost: purchaseUnitCost,
      note: _noteController.text.trim(),
    );
  }

  Future<void> _handleCreateItem() async {
    final ratio = int.tryParse(_createRatioController.text.trim());
    final reorderLevel =
        int.tryParse(_createReorderLevelController.text.trim());
    final latestCost = _createCostController.text.trim().isEmpty
        ? null
        : double.tryParse(_createCostController.text.trim());

    if (_createSkuController.text.trim().isEmpty ||
        _createNameController.text.trim().isEmpty ||
        _createUnitController.text.trim().isEmpty ||
        _createPurchaseUnitController.text.trim().isEmpty) {
      setState(() {
        _localError = '請完整填寫 SKU、名稱、庫存單位與採購單位。';
      });
      return;
    }

    if (ratio == null || ratio <= 0) {
      setState(() {
        _localError = '換算比例必須大於 0。';
      });
      return;
    }

    if (reorderLevel == null || reorderLevel < 0) {
      setState(() {
        _localError = '補貨線不可小於 0。';
      });
      return;
    }

    if (_createCostController.text.trim().isNotEmpty &&
        (latestCost == null || latestCost < 0)) {
      setState(() {
        _localError = '最近採購成本格式不正確。';
      });
      return;
    }

    setState(() {
      _localError = '';
    });

    await widget.onCreateItem(
      type: widget.itemType,
      sku: _createSkuController.text.trim(),
      name: _createNameController.text.trim(),
      unit: _createUnitController.text.trim(),
      purchaseUnit: _createPurchaseUnitController.text.trim(),
      purchaseToStockRatio: ratio,
      reorderLevel: reorderLevel,
      description: _createDescriptionController.text.trim(),
      latestUnitCost: latestCost,
    );

    _clearCreateForm();
  }

  void _clearCreateForm() {
    _createSkuController.clear();
    _createNameController.clear();
    _createUnitController.clear();
    _createPurchaseUnitController.clear();
    _createRatioController.text = '1';
    _createReorderLevelController.text = '0';
    _createDescriptionController.clear();
    _createCostController.clear();
  }

  InputDecoration _inputDecoration(String hintText) {
    return InputDecoration(
      hintText: hintText,
      hintStyle: const TextStyle(color: Colors.white38),
      filled: true,
      fillColor: const Color(0xFF182131),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: BorderSide.none,
      ),
    );
  }
}

class _ReceiveField extends StatelessWidget {
  const _ReceiveField({
    required this.label,
    required this.child,
    this.helper,
  });

  final String label;
  final String? helper;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.w700,
          ),
        ),
        if (helper != null) ...[
          const SizedBox(height: 4),
          Text(
            helper!,
            style: const TextStyle(
              color: Colors.white54,
              fontSize: 12,
            ),
          ),
        ],
        const SizedBox(height: 8),
        child,
      ],
    );
  }
}

class _TypeToggleChip extends StatelessWidget {
  const _TypeToggleChip({
    required this.label,
    required this.active,
    required this.onTap,
  });

  final String label;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Ink(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(14),
          color: active ? const Color(0xFF14F1FF) : const Color(0xFF1A2332),
        ),
        child: Center(
          child: Text(
            label,
            style: TextStyle(
              color: active ? const Color(0xFF07111C) : Colors.white,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
      ),
    );
  }
}

class _InfoPill extends StatelessWidget {
  const _InfoPill({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFF172132),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFF253043)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            label,
            style: const TextStyle(
              color: Color(0xFF8DA2BD),
              fontSize: 11,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _CurrentOrderPanel extends StatelessWidget {
  const _CurrentOrderPanel({
    required this.controller,
    required this.onCheckout,
    this.compact = false,
  });

  final SessionController controller;
  final Future<void> Function() onCheckout;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final cart = controller.cart;
    final subtotal = controller.cartSubtotal;
    const tax = 0.0;
    final total = subtotal + tax;

    return Container(
      color: const Color(0xFF050709),
      padding: EdgeInsets.fromLTRB(
        compact ? 14 : 18,
        compact ? 14 : 18,
        compact ? 14 : 18,
        compact ? 16 : 20,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Current Order',
            style: TextStyle(
              color: const Color(0xFF14F1FF),
              fontSize: compact ? 16 : 18,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            controller.session?.storeCode ?? 'Store Session',
            style: TextStyle(
              color: Colors.white70,
              fontSize: compact ? 11.5 : 13,
            ),
          ),
          SizedBox(height: compact ? 12 : 18),
          Expanded(
            child: cart.isEmpty
                ? Container(
                    width: double.infinity,
                    decoration: BoxDecoration(
                      color: const Color(0xFF10151D),
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: const Color(0xFF1C2432)),
                    ),
                    alignment: Alignment.center,
                    child: const Text(
                      'Cart is empty',
                      style: TextStyle(color: Colors.white60),
                    ),
                  )
                : ListView.separated(
                    itemCount: cart.length,
                    separatorBuilder: (_, __) =>
                        SizedBox(height: compact ? 10 : 12),
                    itemBuilder: (context, index) {
                      final line = cart[index];
                      return _OrderCard(
                        line: line,
                        onDecrease: () => controller.decreaseQuantity(line.key),
                        onIncrease: () => controller.increaseQuantity(line.key),
                        onRemove: () => controller.removeProduct(line.key),
                        compact: compact,
                      );
                    },
                  ),
          ),
          SizedBox(height: compact ? 12 : 16),
          _TotalRow(label: 'Subtotal', value: _currency(subtotal)),
          const SizedBox(height: 10),
          _TotalRow(label: 'Tax', value: _currency(tax)),
          const Divider(color: Color(0xFF202A39), height: 28),
          _TotalRow(
            label: 'Total',
            value: _currency(total),
            emphasize: true,
          ),
          const SizedBox(height: 18),
          SizedBox(
            width: double.infinity,
            height: compact ? 54 : 48,
            child: DecoratedBox(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(14),
                gradient: const LinearGradient(
                  colors: [Color(0xFF1FE4FF), Color(0xFF785BFF)],
                ),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x3314F1FF),
                    blurRadius: 16,
                    offset: Offset(0, 8),
                  ),
                ],
              ),
              child: FilledButton.icon(
                onPressed: cart.isEmpty || controller.checkoutLoading
                    ? null
                    : onCheckout,
                style: FilledButton.styleFrom(
                  backgroundColor: Colors.transparent,
                  shadowColor: Colors.transparent,
                  foregroundColor: const Color(0xFF07111C),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                icon: controller.checkoutLoading
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.credit_card_rounded),
                label: Text(
                  controller.checkoutLoading ? 'PROCESSING' : 'CHECKOUT',
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _OrderCard extends StatelessWidget {
  const _OrderCard({
    required this.line,
    required this.onDecrease,
    required this.onIncrease,
    required this.onRemove,
    required this.compact,
  });

  final PosCartLine line;
  final VoidCallback onDecrease;
  final VoidCallback onIncrease;
  final VoidCallback onRemove;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(compact ? 12 : 16),
      decoration: BoxDecoration(
        color: const Color(0xFF232E40),
        borderRadius: BorderRadius.circular(compact ? 14 : 18),
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
                      style: TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w700,
                        fontSize: compact ? 14 : 16,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      _currency(line.unitPrice),
                      style: TextStyle(
                        color: const Color(0xFF7ED8FF),
                        fontWeight: FontWeight.w600,
                        fontSize: compact ? 12 : 14,
                      ),
                    ),
                    if (line.selectedOptions.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      ...line.selectedOptions.map(
                        (selection) => Padding(
                          padding: const EdgeInsets.only(bottom: 4),
                          child: Text(
                            '${selection.groupName}: ${selection.optionName}'
                            '${selection.priceDelta > 0 ? ' (+${_currency(selection.priceDelta)})' : ''}',
                            style: TextStyle(
                              color: Colors.white60,
                              fontSize: compact ? 10.5 : 12,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              IconButton(
                onPressed: onRemove,
                icon: const Icon(
                  Icons.delete_outline_rounded,
                  color: Color(0xFFFF6E79),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              _CircleButton(
                icon: Icons.remove_rounded,
                onTap: onDecrease,
                compact: compact,
              ),
              Padding(
                padding: EdgeInsets.symmetric(horizontal: compact ? 10 : 12),
                child: Text(
                  '${line.quantity}',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: compact ? 16 : 18,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              _CircleButton(
                icon: Icons.add_rounded,
                onTap: onIncrease,
                compact: compact,
              ),
              const Spacer(),
              Text(
                _currency(line.lineTotal),
                style: TextStyle(
                  color: const Color(0xFF14F1FF),
                  fontWeight: FontWeight.w800,
                  fontSize: compact ? 18 : 20,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _CircleButton extends StatelessWidget {
  const _CircleButton({
    required this.icon,
    required this.onTap,
    required this.compact,
  });

  final IconData icon;
  final VoidCallback onTap;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(999),
      child: Ink(
        width: compact ? 30 : 34,
        height: compact ? 30 : 34,
        decoration: const BoxDecoration(
          shape: BoxShape.circle,
          color: Color(0xFF364154),
        ),
        child: Icon(icon, size: compact ? 16 : 18, color: Colors.white),
      ),
    );
  }
}

class _PrinterPanel extends StatelessWidget {
  const _PrinterPanel({required this.printerController});

  final PrinterController printerController;

  @override
  Widget build(BuildContext context) {
    final selected = printerController.selectedPrinter;

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFF172132),
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: const Color(0xFF243047)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Printer Settings',
            style: TextStyle(
              fontWeight: FontWeight.w800,
              fontSize: 18,
            ),
          ),
          const SizedBox(height: 10),
          Text(
            selected == null
                ? 'No printer selected'
                : 'Selected: ${selected.name ?? selected.address ?? 'Unnamed Printer'}',
            style: const TextStyle(color: Colors.white70),
          ),
          const SizedBox(height: 6),
          Text(
            selected == null
                ? 'Supports Bluetooth BLE and USB thermal printers'
                : 'Status: ${(selected.isConnected ?? false) ? 'Connected' : 'Disconnected'} / ${selected.connectionTypeString}',
            style: const TextStyle(color: Colors.white60),
          ),
          if (printerController.statusMessage.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(
              printerController.statusMessage,
              style: const TextStyle(color: Color(0xFF14F1FF)),
            ),
          ],
          if (printerController.errorMessage.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(
              printerController.errorMessage,
              style: const TextStyle(color: Colors.redAccent),
            ),
          ],
          const SizedBox(height: 14),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              FilledButton.icon(
                onPressed: printerController.scanning
                    ? null
                    : printerController.startScan,
                icon: const Icon(Icons.radar_rounded),
                label: const Text('Scan'),
              ),
              OutlinedButton.icon(
                onPressed: printerController.scanning
                    ? printerController.stopScan
                    : null,
                icon: const Icon(Icons.stop_circle_outlined),
                label: const Text('Stop'),
              ),
              OutlinedButton.icon(
                onPressed: printerController.printing || selected == null
                    ? null
                    : printerController.printTestReceipt,
                icon: const Icon(Icons.receipt_long_rounded),
                label: const Text('Test Print'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Auto print after checkout'),
            value: printerController.autoPrintReceipt,
            onChanged: printerController.setAutoPrintReceipt,
          ),
          const SizedBox(height: 8),
          if (printerController.printers.isEmpty)
            const Text(
              'No printers found yet. Tap Scan to search nearby devices.',
              style: TextStyle(color: Colors.white54),
            )
          else
            Column(
              children: printerController.printers
                  .map(
                    (printer) => Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: _PrinterDeviceTile(
                        printer: printer,
                        selected:
                            selected != null && _samePrinter(printer, selected),
                        connecting: printerController.connecting,
                        onTap: () => printerController.connectPrinter(printer),
                      ),
                    ),
                  )
                  .toList(growable: false),
            ),
        ],
      ),
    );
  }

  bool _samePrinter(Printer left, Printer right) {
    final leftAddress = left.address?.trim();
    final rightAddress = right.address?.trim();
    if (left.connectionTypeString == right.connectionTypeString &&
        leftAddress != null &&
        rightAddress != null &&
        leftAddress.isNotEmpty &&
        rightAddress.isNotEmpty) {
      return leftAddress == rightAddress;
    }

    return (left.name ?? '').trim() == (right.name ?? '').trim() &&
        left.connectionTypeString == right.connectionTypeString;
  }
}

class _PrinterDeviceTile extends StatelessWidget {
  const _PrinterDeviceTile({
    required this.printer,
    required this.selected,
    required this.connecting,
    required this.onTap,
  });

  final Printer printer;
  final bool selected;
  final bool connecting;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: selected ? const Color(0xFF223047) : const Color(0xFF101827),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: selected ? const Color(0xFF14F1FF) : const Color(0xFF243047),
        ),
      ),
      child: Row(
        children: [
          Icon(
            printer.connectionTypeString == 'USB'
                ? Icons.usb_rounded
                : Icons.bluetooth_rounded,
            color: const Color(0xFF14F1FF),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  printer.name?.trim().isNotEmpty == true
                      ? printer.name!
                      : (printer.address ?? 'Unnamed Printer'),
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 4),
                Text(
                  printer.connectionTypeString,
                  style: const TextStyle(color: Colors.white60),
                ),
              ],
            ),
          ),
          FilledButton(
            onPressed: connecting ? null : onTap,
            child: Text(selected ? 'Selected' : 'Use'),
          ),
        ],
      ),
    );
  }
}

class _BannerMessage extends StatelessWidget {
  const _BannerMessage({
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

class _TotalRow extends StatelessWidget {
  const _TotalRow({
    required this.label,
    required this.value,
    this.emphasize = false,
  });

  final String label;
  final String value;
  final bool emphasize;

  @override
  Widget build(BuildContext context) {
    final color = emphasize ? const Color(0xFF14F1FF) : Colors.white;
    final size = emphasize ? 28.0 : 15.0;
    final weight = emphasize ? FontWeight.w800 : FontWeight.w600;

    return Row(
      children: [
        Text(
          label,
          style: TextStyle(
            color: Colors.white,
            fontSize: emphasize ? 18 : 14,
            fontWeight: FontWeight.w700,
          ),
        ),
        const Spacer(),
        Text(
          value,
          style: TextStyle(
            color: color,
            fontSize: size,
            fontWeight: weight,
          ),
        ),
      ],
    );
  }
}

String _currency(double value) => '\$${value.toStringAsFixed(2)}';

class _CustomizationSheet extends StatefulWidget {
  const _CustomizationSheet({required this.product});

  final ProductSummary product;

  @override
  State<_CustomizationSheet> createState() => _CustomizationSheetState();
}

class _CustomizationSheetState extends State<_CustomizationSheet> {
  late final Map<String, Set<String>> _selectedOptionIdsByGroup;
  String _errorMessage = '';

  @override
  void initState() {
    super.initState();
    _selectedOptionIdsByGroup = {
      for (final group in widget.product.customizationGroups)
        group.id: group.options
            .where((option) => option.defaultSelected)
            .map((option) => option.id)
            .toSet(),
    };
  }

  @override
  Widget build(BuildContext context) {
    final selections = _buildSelections();
    final extraTotal = selections.fold<double>(
      0,
      (sum, selection) => sum + selection.priceDelta,
    );
    final previewPrice = widget.product.price + extraTotal;

    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          20,
          20,
          20,
          20 + MediaQuery.of(context).viewInsets.bottom,
        ),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                widget.product.name,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 22,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Base price ${_currency(widget.product.price)}',
                style: const TextStyle(color: Color(0xFF8DA2BD)),
              ),
              if (_errorMessage.isNotEmpty) ...[
                const SizedBox(height: 12),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: const Color(0xFF35161B),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: const Color(0xFFB74B57)),
                  ),
                  child: Text(_errorMessage),
                ),
              ],
              const SizedBox(height: 18),
              ...widget.product.customizationGroups.map(_buildGroupCard),
              const SizedBox(height: 16),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: const Color(0xFF172132),
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(color: const Color(0xFF253043)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Price Preview',
                      style: TextStyle(
                        color: Color(0xFF8DA2BD),
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _currency(previewPrice),
                      style: const TextStyle(
                        color: Color(0xFF14F1FF),
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text('Cancel'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton(
                      onPressed: () {
                        if (!_validateSelections()) {
                          return;
                        }
                        Navigator.of(context).pop(_buildSelections());
                      },
                      child: const Text('Add To Cart'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildGroupCard(ProductCustomizationGroup group) {
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFF172132),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFF253043)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            group.name,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 16,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            group.isSingleSelect
                ? 'Choose up to ${group.maxSelections}'
                : 'Choose ${group.minSelections} - ${group.maxSelections}',
            style: const TextStyle(color: Color(0xFF8DA2BD), fontSize: 12),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: group.options.map((option) {
              final active =
                  _selectedOptionIdsByGroup[group.id]?.contains(option.id) ??
                      false;
              return InkWell(
                onTap: () => _toggleOption(group, option),
                borderRadius: BorderRadius.circular(14),
                child: Ink(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 10,
                  ),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(14),
                    color: active
                        ? const Color(0xFF14F1FF)
                        : const Color(0xFF223047),
                    border: Border.all(
                      color: active
                          ? const Color(0xFF14F1FF)
                          : const Color(0xFF304056),
                    ),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        option.name,
                        style: TextStyle(
                          color:
                              active ? const Color(0xFF07111C) : Colors.white,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        option.priceDelta > 0
                            ? '+${_currency(option.priceDelta)}'
                            : 'No extra charge',
                        style: TextStyle(
                          color: active
                              ? const Color(0xFF07111C)
                              : const Color(0xFF9AB0C7),
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }).toList(growable: false),
          ),
        ],
      ),
    );
  }

  void _toggleOption(
    ProductCustomizationGroup group,
    ProductCustomizationOption option,
  ) {
    final current = <String>{
      ...(_selectedOptionIdsByGroup[group.id] ?? const <String>{}),
    };

    setState(() {
      _errorMessage = '';
      if (group.isSingleSelect) {
        if (current.contains(option.id)) {
          current.remove(option.id);
        } else {
          current
            ..clear()
            ..add(option.id);
        }
      } else {
        if (current.contains(option.id)) {
          current.remove(option.id);
        } else if (current.length < group.maxSelections) {
          current.add(option.id);
        } else {
          _errorMessage =
              'This group allows up to ${group.maxSelections} options.';
        }
      }
      _selectedOptionIdsByGroup[group.id] = current;
    });
  }

  bool _validateSelections() {
    for (final group in widget.product.customizationGroups) {
      final count = _selectedOptionIdsByGroup[group.id]?.length ?? 0;
      if (group.required && count == 0) {
        setState(() {
          _errorMessage = '${group.name} is required.';
        });
        return false;
      }
      if (count < group.minSelections) {
        setState(() {
          _errorMessage =
              '${group.name} requires at least ${group.minSelections} selection(s).';
        });
        return false;
      }
      if (count > group.maxSelections) {
        setState(() {
          _errorMessage =
              '${group.name} allows up to ${group.maxSelections} selection(s).';
        });
        return false;
      }
    }
    return true;
  }

  List<PosCartSelection> _buildSelections() {
    final selections = <PosCartSelection>[];
    for (final group in widget.product.customizationGroups) {
      final selectedIds =
          _selectedOptionIdsByGroup[group.id] ?? const <String>{};
      for (final option
          in group.options.where((item) => selectedIds.contains(item.id))) {
        selections.add(
          PosCartSelection(
            groupId: group.id,
            groupName: group.name,
            optionId: option.id,
            optionName: option.name,
            priceDelta: option.priceDelta,
          ),
        );
      }
    }
    return selections;
  }
}
