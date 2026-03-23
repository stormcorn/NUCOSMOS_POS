package com.nucosmos.pos.backend.report;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.inventory.persistence.InventoryMovementEntity;
import com.nucosmos.pos.backend.inventory.persistence.InventoryStockEntity;
import com.nucosmos.pos.backend.inventory.repository.InventoryMovementRepository;
import com.nucosmos.pos.backend.inventory.repository.InventoryStockRepository;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;
import com.nucosmos.pos.backend.order.persistence.PaymentEntity;
import com.nucosmos.pos.backend.order.repository.OrderRepository;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import com.nucosmos.pos.backend.supply.persistence.MaterialItemEntity;
import com.nucosmos.pos.backend.supply.persistence.MaterialStockLotEntity;
import com.nucosmos.pos.backend.supply.persistence.MaterialMovementEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingItemEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingStockLotEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingMovementEntity;
import com.nucosmos.pos.backend.supply.repository.MaterialItemRepository;
import com.nucosmos.pos.backend.supply.repository.MaterialStockLotRepository;
import com.nucosmos.pos.backend.supply.repository.MaterialMovementRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingItemRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingStockLotRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final MaterialItemRepository materialItemRepository;
    private final MaterialMovementRepository materialMovementRepository;
    private final MaterialStockLotRepository materialStockLotRepository;
    private final PackagingItemRepository packagingItemRepository;
    private final PackagingMovementRepository packagingMovementRepository;
    private final PackagingStockLotRepository packagingStockLotRepository;

    public ReportService(
            OrderRepository orderRepository,
            StoreRepository storeRepository,
            InventoryStockRepository inventoryStockRepository,
            InventoryMovementRepository inventoryMovementRepository,
            MaterialItemRepository materialItemRepository,
            MaterialMovementRepository materialMovementRepository,
            MaterialStockLotRepository materialStockLotRepository,
            PackagingItemRepository packagingItemRepository,
            PackagingMovementRepository packagingMovementRepository,
            PackagingStockLotRepository packagingStockLotRepository
    ) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.materialItemRepository = materialItemRepository;
        this.materialMovementRepository = materialMovementRepository;
        this.materialStockLotRepository = materialStockLotRepository;
        this.packagingItemRepository = packagingItemRepository;
        this.packagingMovementRepository = packagingMovementRepository;
        this.packagingStockLotRepository = packagingStockLotRepository;
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse salesSummary(AuthenticatedUser user, OffsetDateTime from, OffsetDateTime to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be before or equal to to");
        }

        List<OrderEntity> orders = orderRepository.findAllByStore_CodeAndOrderedAtBetweenOrderByOrderedAtAsc(
                user.storeCode(),
                from,
                to
        );

        BigDecimal grossSalesAmount = BigDecimal.ZERO;
        BigDecimal refundedAmount = BigDecimal.ZERO;
        BigDecimal cashSalesAmount = BigDecimal.ZERO;
        BigDecimal cardSalesAmount = BigDecimal.ZERO;
        int orderCount = 0;
        int voidedOrderCount = 0;

        for (OrderEntity order : orders) {
            if ("VOIDED".equals(order.getStatus())) {
                voidedOrderCount++;
            } else {
                orderCount++;
                grossSalesAmount = grossSalesAmount.add(order.getTotalAmount());
                refundedAmount = refundedAmount.add(order.getRefundedAmount());
            }

            for (PaymentEntity payment : order.getPayments()) {
                if (!isSettledPayment(payment)) {
                    continue;
                }
                if ("CASH".equals(payment.getPaymentMethod())) {
                    cashSalesAmount = cashSalesAmount.add(payment.getAmount());
                } else if ("CARD".equals(payment.getPaymentMethod())) {
                    cardSalesAmount = cardSalesAmount.add(payment.getAmount());
                }
            }
        }

        BigDecimal netSalesAmount = grossSalesAmount.subtract(refundedAmount);
        BigDecimal averageOrderAmount = orderCount == 0
                ? BigDecimal.ZERO
                : netSalesAmount.divide(BigDecimal.valueOf(orderCount), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal cogsAmount = orders.stream()
                .filter(order -> !"VOIDED".equals(order.getStatus()))
                .map(OrderEntity::getCogsAmount)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        BigDecimal refundedCogsAmount = orders.stream()
                .filter(order -> !"VOIDED".equals(order.getStatus()))
                .map(OrderEntity::getRefundedCogsAmount)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        BigDecimal netCogsAmount = cogsAmount.subtract(refundedCogsAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossProfitAmount = netSalesAmount.subtract(netCogsAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossMarginRate = netSalesAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : grossProfitAmount.multiply(BigDecimal.valueOf(100))
                        .divide(netSalesAmount, 2, RoundingMode.HALF_UP);

        return new SalesSummaryResponse(
                user.storeCode(),
                from,
                to,
                orderCount,
                voidedOrderCount,
                grossSalesAmount,
                refundedAmount,
                netSalesAmount,
                cashSalesAmount,
                cardSalesAmount,
                averageOrderAmount,
                cogsAmount,
                refundedCogsAmount,
                netCogsAmount,
                grossProfitAmount,
                grossMarginRate
        );
    }

    @Transactional(readOnly = true)
    public InventoryAnalyticsResponse inventoryAnalytics(AuthenticatedUser user, OffsetDateTime from, OffsetDateTime to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be before or equal to to");
        }

        StoreEntity store = resolveActiveStore(user);

        List<InventoryStockEntity> productStocks = inventoryStockRepository.findAllByStore_IdOrderByProduct_Category_DisplayOrderAscProduct_NameAsc(store.getId());
        List<MaterialItemEntity> materialItems = materialItemRepository.findAllByStore_CodeOrderByActiveDescNameAsc(user.storeCode())
                .stream()
                .filter(MaterialItemEntity::isActive)
                .toList();
        List<PackagingItemEntity> packagingItems = packagingItemRepository.findAllByStore_CodeOrderByActiveDescNameAsc(user.storeCode())
                .stream()
                .filter(PackagingItemEntity::isActive)
                .toList();

        List<InventoryMovementEntity> productMovements = inventoryMovementRepository.findAllByStore_CodeAndOccurredAtBetweenOrderByOccurredAtAsc(user.storeCode(), from, to);
        List<MaterialMovementEntity> materialMovements = materialMovementRepository.findAllByMaterial_Store_CodeAndOccurredAtBetweenOrderByOccurredAtAsc(user.storeCode(), from, to);
        List<PackagingMovementEntity> packagingMovements = packagingMovementRepository.findAllByPackagingItem_Store_CodeAndOccurredAtBetweenOrderByOccurredAtAsc(user.storeCode(), from, to);
        List<MaterialStockLotEntity> materialLots = materialStockLotRepository.findAllByMaterial_Store_CodeOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(user.storeCode());
        List<PackagingStockLotEntity> packagingLots = packagingStockLotRepository.findAllByPackagingItem_Store_CodeOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(user.storeCode());

        InventoryKpiSummaryResponse summary = new InventoryKpiSummaryResponse(
                productStocks.size(),
                (int) productStocks.stream().filter(stock -> stock.getSellableQuantity() <= stock.getReorderLevel()).count(),
                productStocks.stream().mapToInt(InventoryStockEntity::getSellableQuantity).sum(),
                productStocks.stream().mapToInt(InventoryStockEntity::getDefectiveQuantity).sum(),
                materialItems.size(),
                (int) materialItems.stream().filter(item -> item.getQuantityOnHand() <= item.getReorderLevel()).count(),
                materialItems.stream().mapToInt(MaterialItemEntity::getQuantityOnHand).sum(),
                packagingItems.size(),
                (int) packagingItems.stream().filter(item -> item.getQuantityOnHand() <= item.getReorderLevel()).count(),
                packagingItems.stream().mapToInt(PackagingItemEntity::getQuantityOnHand).sum()
        );

        List<LowStockSnapshotResponse> lowStockProducts = productStocks.stream()
                .filter(stock -> stock.getSellableQuantity() <= stock.getReorderLevel())
                .map(stock -> new LowStockSnapshotResponse(
                        "PRODUCT",
                        stock.getProduct().getSku(),
                        stock.getProduct().getName(),
                        stock.getProduct().getCategory().getName(),
                        "items",
                        stock.getSellableQuantity(),
                        stock.getReorderLevel()
                ))
                .toList();

        List<LowStockSnapshotResponse> lowStockMaterials = materialItems.stream()
                .filter(item -> item.getQuantityOnHand() <= item.getReorderLevel())
                .map(item -> new LowStockSnapshotResponse(
                        "MATERIAL",
                        item.getSku(),
                        item.getName(),
                        null,
                        item.getUnit(),
                        item.getQuantityOnHand(),
                        item.getReorderLevel()
                ))
                .toList();

        List<LowStockSnapshotResponse> lowStockPackaging = packagingItems.stream()
                .filter(item -> item.getQuantityOnHand() <= item.getReorderLevel())
                .map(item -> new LowStockSnapshotResponse(
                        "PACKAGING",
                        item.getSku(),
                        item.getName(),
                        item.getSpecification(),
                        item.getUnit(),
                        item.getQuantityOnHand(),
                        item.getReorderLevel()
                ))
                .toList();

        return new InventoryAnalyticsResponse(
                user.storeCode(),
                from,
                to,
                summary,
                lowStockProducts,
                lowStockMaterials,
                lowStockPackaging,
                summarizeMovementTotals("PRODUCT", productMovements, InventoryMovementEntity::getMovementType, InventoryMovementEntity::getQuantity, InventoryMovementEntity::getQuantityDelta),
                summarizeMovementTotals("MATERIAL", materialMovements, MaterialMovementEntity::getMovementType, MaterialMovementEntity::getQuantity, MaterialMovementEntity::getQuantityDelta),
                summarizeMovementTotals("PACKAGING", packagingMovements, PackagingMovementEntity::getMovementType, PackagingMovementEntity::getQuantity, PackagingMovementEntity::getQuantityDelta),
                summarizeConsumption(materialMovements, MaterialMovementEntity::getMovementType, MaterialMovementEntity::getQuantity, MaterialMovementEntity::getUnitCost,
                        movement -> movement.getMaterial().getSku(),
                        movement -> movement.getMaterial().getName(),
                        movement -> movement.getMaterial().getUnit(),
                        "MATERIAL"),
                summarizeConsumption(packagingMovements, PackagingMovementEntity::getMovementType, PackagingMovementEntity::getQuantity, PackagingMovementEntity::getUnitCost,
                        movement -> movement.getPackagingItem().getSku(),
                        movement -> movement.getPackagingItem().getName(),
                        movement -> movement.getPackagingItem().getUnit(),
                        "PACKAGING"),
                summarizeDefectiveAndWaste(productMovements),
                summarizeExpiringLots(
                        materialLots,
                        lot -> lot.getMaterial().getSku(),
                        lot -> lot.getMaterial().getName(),
                        lot -> lot.getMaterial().getUnit(),
                        "MATERIAL"
                ),
                summarizeExpiringLots(
                        packagingLots,
                        lot -> lot.getPackagingItem().getSku(),
                        lot -> lot.getPackagingItem().getName(),
                        lot -> lot.getPackagingItem().getUnit(),
                        "PACKAGING"
                )
        );
    }

    @Transactional(readOnly = true)
    public SalesTrendResponse salesTrend(AuthenticatedUser user, OffsetDateTime from, OffsetDateTime to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be before or equal to to");
        }

        StoreEntity store = resolveActiveStore(user);
        ZoneId zoneId = ZoneId.of(store.getTimezone());
        List<OrderEntity> orders = orderRepository.findAllByStore_CodeAndOrderedAtBetweenOrderByOrderedAtAsc(
                user.storeCode(),
                from,
                to
        );

        record TrendAccumulator(int orderCount, BigDecimal gross, BigDecimal refunded, BigDecimal net) {
            private static TrendAccumulator empty() {
                return new TrendAccumulator(
                        0,
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                );
            }

            private TrendAccumulator add(OrderEntity order) {
                if ("VOIDED".equals(order.getStatus())) {
                    return this;
                }

                BigDecimal grossAmount = order.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
                BigDecimal refundedAmount = order.getRefundedAmount().setScale(2, RoundingMode.HALF_UP);
                BigDecimal netAmount = grossAmount.subtract(refundedAmount).setScale(2, RoundingMode.HALF_UP);

                return new TrendAccumulator(
                        orderCount + 1,
                        gross.add(grossAmount).setScale(2, RoundingMode.HALF_UP),
                        refunded.add(refundedAmount).setScale(2, RoundingMode.HALF_UP),
                        net.add(netAmount).setScale(2, RoundingMode.HALF_UP)
                );
            }
        }

        Map<LocalDate, TrendAccumulator> grouped = orders.stream()
                .collect(Collectors.toMap(
                        order -> order.getOrderedAt().atZoneSameInstant(zoneId).toLocalDate(),
                        order -> TrendAccumulator.empty().add(order),
                        (left, right) -> new TrendAccumulator(
                                left.orderCount() + right.orderCount(),
                                left.gross().add(right.gross()).setScale(2, RoundingMode.HALF_UP),
                                left.refunded().add(right.refunded()).setScale(2, RoundingMode.HALF_UP),
                                left.net().add(right.net()).setScale(2, RoundingMode.HALF_UP)
                        )
                ));

        LocalDate startDate = from.atZoneSameInstant(zoneId).toLocalDate();
        LocalDate endDate = to.atZoneSameInstant(zoneId).toLocalDate();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MM/dd");

        List<SalesTrendPointResponse> points = new ArrayList<>();
        for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
            TrendAccumulator accumulator = grouped.getOrDefault(cursor, TrendAccumulator.empty());
            points.add(new SalesTrendPointResponse(
                    cursor.format(labelFormatter),
                    cursor.atStartOfDay(zoneId).toOffsetDateTime().toString(),
                    accumulator.orderCount(),
                    accumulator.gross(),
                    accumulator.refunded(),
                    accumulator.net()
            ));
        }

        return new SalesTrendResponse(
                user.storeCode(),
                from,
                to,
                "DAY",
                points
        );
    }

    private StoreEntity resolveActiveStore(AuthenticatedUser user) {
        return storeRepository.findByCodeAndStatus(user.storeCode(), "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated store is not available"));
    }

    private boolean isSettledPayment(PaymentEntity payment) {
        return "CAPTURED".equals(payment.getStatus()) || "REFUNDED".equals(payment.getStatus());
    }

    private <T> List<MovementTotalResponse> summarizeMovementTotals(
            String scope,
            List<T> movements,
            Function<T, String> movementTypeExtractor,
            Function<T, Integer> quantityExtractor,
            Function<T, Integer> deltaExtractor
    ) {
        Map<String, List<T>> grouped = movements.stream()
                .collect(Collectors.groupingBy(movementTypeExtractor));

        return grouped.entrySet().stream()
                .map(entry -> new MovementTotalResponse(
                        scope,
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(quantityExtractor).mapToInt(Integer::intValue).sum(),
                        entry.getValue().stream().map(deltaExtractor).mapToInt(Integer::intValue).sum()
                ))
                .sorted(java.util.Comparator.comparing(MovementTotalResponse::movementType))
                .toList();
    }

    private <T> List<ConsumptionSummaryResponse> summarizeConsumption(
            List<T> movements,
            Function<T, String> movementTypeExtractor,
            Function<T, Integer> quantityExtractor,
            Function<T, BigDecimal> unitCostExtractor,
            Function<T, String> skuExtractor,
            Function<T, String> nameExtractor,
            Function<T, String> unitExtractor,
            String scope
    ) {
        record ConsumptionAccumulator(String scope, String sku, String name, String unit, int quantity, BigDecimal cost) {
        }

        Map<String, ConsumptionAccumulator> grouped = movements.stream()
                .filter(movement -> "CONSUME_OUT".equals(movementTypeExtractor.apply(movement)))
                .collect(Collectors.toMap(
                        skuExtractor,
                        movement -> new ConsumptionAccumulator(
                                scope,
                                skuExtractor.apply(movement),
                                nameExtractor.apply(movement),
                                unitExtractor.apply(movement),
                                quantityExtractor.apply(movement),
                                calculateCost(quantityExtractor.apply(movement), unitCostExtractor.apply(movement))
                        ),
                        (left, right) -> new ConsumptionAccumulator(
                                left.scope(),
                                left.sku(),
                                left.name(),
                                left.unit(),
                                left.quantity() + right.quantity(),
                                left.cost().add(right.cost()).setScale(2, RoundingMode.HALF_UP)
                        )
                ));

        return grouped.values().stream()
                .map(value -> new ConsumptionSummaryResponse(
                        value.scope(),
                        value.sku(),
                        value.name(),
                        value.unit(),
                        value.quantity(),
                        value.cost()
                ))
                .sorted(java.util.Comparator.comparing(ConsumptionSummaryResponse::consumedQuantity).reversed())
                .toList();
    }

    private List<DefectiveWasteSummaryResponse> summarizeDefectiveAndWaste(List<InventoryMovementEntity> movements) {
        record DefectiveKey(String sku, String name, String movementType) {
        }

        Map<DefectiveKey, Integer> totals = movements.stream()
                .filter(movement -> List.of("REFUND_DEFECT", "DAMAGE_OUT", "SCRAP_OUT").contains(movement.getMovementType()))
                .collect(Collectors.toMap(
                        movement -> new DefectiveKey(
                                movement.getProduct().getSku(),
                                movement.getProduct().getName(),
                                movement.getMovementType()
                        ),
                        InventoryMovementEntity::getQuantity,
                        Integer::sum
                ));

        return totals.entrySet().stream()
                .map(entry -> new DefectiveWasteSummaryResponse(
                        entry.getKey().sku(),
                        entry.getKey().name(),
                        entry.getKey().movementType(),
                        entry.getValue()
                ))
                .sorted(java.util.Comparator.comparing(DefectiveWasteSummaryResponse::affectedQuantity).reversed())
                .toList();
    }

    private <T> List<ExpiringLotSnapshotResponse> summarizeExpiringLots(
            List<T> lots,
            Function<T, String> skuExtractor,
            Function<T, String> nameExtractor,
            Function<T, String> unitExtractor,
            String scope
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.plusDays(14);

        return lots.stream()
                .filter(lot -> remainingQuantity(lot) > 0)
                .filter(lot -> expiryDate(lot) != null && !expiryDate(lot).isAfter(threshold))
                .map(lot -> new ExpiringLotSnapshotResponse(
                        scope,
                        skuExtractor.apply(lot),
                        nameExtractor.apply(lot),
                        batchCode(lot),
                        expiryDate(lot),
                        remainingQuantity(lot),
                        unitExtractor.apply(lot),
                        java.time.Duration.between(now, expiryDate(lot)).toDays()
                ))
                .sorted(java.util.Comparator.comparing(ExpiringLotSnapshotResponse::expiryDate))
                .toList();
    }

    private OffsetDateTime expiryDate(Object lot) {
        if (lot instanceof MaterialStockLotEntity materialLot) {
            return materialLot.getExpiryDate();
        }
        if (lot instanceof PackagingStockLotEntity packagingLot) {
            return packagingLot.getExpiryDate();
        }
        return null;
    }

    private String batchCode(Object lot) {
        if (lot instanceof MaterialStockLotEntity materialLot) {
            return materialLot.getBatchCode();
        }
        if (lot instanceof PackagingStockLotEntity packagingLot) {
            return packagingLot.getBatchCode();
        }
        return null;
    }

    private int remainingQuantity(Object lot) {
        if (lot instanceof MaterialStockLotEntity materialLot) {
            return materialLot.getRemainingQuantity();
        }
        if (lot instanceof PackagingStockLotEntity packagingLot) {
            return packagingLot.getRemainingQuantity();
        }
        return 0;
    }

    private BigDecimal calculateCost(int quantity, BigDecimal unitCost) {
        if (unitCost == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return unitCost.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
