package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.inventory.InventoryMovementType;
import com.nucosmos.pos.backend.inventory.InventoryService;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;
import com.nucosmos.pos.backend.order.persistence.OrderItemEntity;
import com.nucosmos.pos.backend.order.persistence.RefundEntity;
import com.nucosmos.pos.backend.order.persistence.RefundItemEntity;
import com.nucosmos.pos.backend.order.repository.RefundItemRepository;
import com.nucosmos.pos.backend.product.persistence.ProductMaterialRecipeEntity;
import com.nucosmos.pos.backend.product.persistence.ProductPackagingRecipeEntity;
import com.nucosmos.pos.backend.product.repository.ProductMaterialRecipeRepository;
import com.nucosmos.pos.backend.product.repository.ProductPackagingRecipeRepository;
import com.nucosmos.pos.backend.supply.SupplyMovementType;
import com.nucosmos.pos.backend.supply.persistence.MaterialItemEntity;
import com.nucosmos.pos.backend.supply.persistence.MaterialMovementEntity;
import com.nucosmos.pos.backend.supply.persistence.MaterialStockLotEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingItemEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingMovementEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingStockLotEntity;
import com.nucosmos.pos.backend.supply.repository.MaterialMovementRepository;
import com.nucosmos.pos.backend.supply.repository.MaterialStockLotRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingMovementRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingStockLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderInventoryWorkflowService {

    private final InventoryService inventoryService;
    private final ProductMaterialRecipeRepository productMaterialRecipeRepository;
    private final ProductPackagingRecipeRepository productPackagingRecipeRepository;
    private final MaterialMovementRepository materialMovementRepository;
    private final PackagingMovementRepository packagingMovementRepository;
    private final MaterialStockLotRepository materialStockLotRepository;
    private final PackagingStockLotRepository packagingStockLotRepository;
    private final RefundItemRepository refundItemRepository;

    public OrderInventoryWorkflowService(
            InventoryService inventoryService,
            ProductMaterialRecipeRepository productMaterialRecipeRepository,
            ProductPackagingRecipeRepository productPackagingRecipeRepository,
            MaterialMovementRepository materialMovementRepository,
            PackagingMovementRepository packagingMovementRepository,
            MaterialStockLotRepository materialStockLotRepository,
            PackagingStockLotRepository packagingStockLotRepository,
            RefundItemRepository refundItemRepository
    ) {
        this.inventoryService = inventoryService;
        this.productMaterialRecipeRepository = productMaterialRecipeRepository;
        this.productPackagingRecipeRepository = productPackagingRecipeRepository;
        this.materialMovementRepository = materialMovementRepository;
        this.packagingMovementRepository = packagingMovementRepository;
        this.materialStockLotRepository = materialStockLotRepository;
        this.packagingStockLotRepository = packagingStockLotRepository;
        this.refundItemRepository = refundItemRepository;
    }

    @Transactional
    public void commitOrderInventory(OrderEntity order, UserEntity actor) {
        if (order.isInventoryCommitted()) {
            return;
        }

        OffsetDateTime occurredAt = OffsetDateTime.now();
        BigDecimal totalCogsAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (OrderItemEntity item : order.getItems()) {
            inventoryService.recordSystemMovement(
                    order.getStore(),
                    item.getProduct(),
                    actor,
                    InventoryMovementType.SALE_OUT,
                    item.getQuantity(),
                    null,
                    "ORDER_SALE",
                    "Inventory committed from paid order",
                    "ORDER",
                    order.getId()
            );

            BigDecimal materialCost = consumeMaterialsForOrderItem(item, actor, order.getId(), occurredAt);
            BigDecimal packagingCost = consumePackagingForOrderItem(item, actor, order.getId(), occurredAt);
            BigDecimal lineCost = materialCost.add(packagingCost).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unitCost = item.getQuantity() == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : lineCost.divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);

            item.applyCost(unitCost, lineCost);
            totalCogsAmount = totalCogsAmount.add(lineCost).setScale(2, RoundingMode.HALF_UP);
        }

        order.applyCostOfGoods(totalCogsAmount);
        order.markInventoryCommitted();
    }

    @Transactional
    public void restoreRefundInventory(
            OrderEntity order,
            RefundEntity refund,
            List<RefundItemRequest> requests,
            UserEntity actor
    ) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        Map<UUID, OrderItemEntity> orderItemById = new HashMap<>();
        for (OrderItemEntity item : order.getItems()) {
            orderItemById.put(item.getId(), item);
        }

        Map<UUID, Integer> refundedQuantityByOrderItemId = new HashMap<>();
        for (RefundItemEntity refundItem : refundItemRepository.findAllByRefund_Order_Id(order.getId())) {
            refundedQuantityByOrderItemId.merge(refundItem.getOrderItem().getId(), refundItem.getQuantity(), Integer::sum);
        }

        for (RefundItemRequest request : requests) {
            OrderItemEntity orderItem = orderItemById.get(request.orderItemId());
            if (orderItem == null) {
                throw new BadRequestException("Refund item does not belong to the order");
            }

            int alreadyRefundedQuantity = refundedQuantityByOrderItemId.getOrDefault(orderItem.getId(), 0);
            int remainingRefundableQuantity = orderItem.getQuantity() - alreadyRefundedQuantity;
            if (request.quantity() > remainingRefundableQuantity) {
                throw new BadRequestException("Refund quantity exceeds refundable item quantity");
            }

            RefundInventoryDisposition disposition = RefundInventoryDisposition.from(request.inventoryDisposition());
            RefundItemEntity refundItem = new RefundItemEntity(
                    refund,
                    orderItem,
                    orderItem.getProduct(),
                    request.quantity(),
                    disposition.name()
            );
            refund.addRefundItem(refundItem);
            refundItemRepository.save(refundItem);
            refundedQuantityByOrderItemId.merge(orderItem.getId(), request.quantity(), Integer::sum);

            inventoryService.recordSystemMovement(
                    order.getStore(),
                    orderItem.getProduct(),
                    actor,
                    disposition == RefundInventoryDisposition.DEFECTIVE
                            ? InventoryMovementType.REFUND_DEFECT
                            : InventoryMovementType.REFUND_IN,
                    request.quantity(),
                    null,
                    disposition == RefundInventoryDisposition.DEFECTIVE ? "REFUND_DEFECTIVE" : "REFUND_RETURN",
                    "Inventory restored from refund",
                    "REFUND",
                    refund.getId()
            );

            BigDecimal refundUnitCost = orderItem.getQuantity() == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : orderItem.getLineCostAmount().divide(BigDecimal.valueOf(orderItem.getQuantity()), 6, RoundingMode.HALF_UP);
            BigDecimal additionalRefundedCost = refundUnitCost.multiply(BigDecimal.valueOf(request.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal nextRefundedCost = orderItem.getRefundedCostAmount().add(additionalRefundedCost);
            if (nextRefundedCost.compareTo(orderItem.getLineCostAmount()) > 0) {
                nextRefundedCost = orderItem.getLineCostAmount();
            }
            orderItem.applyRefundedCost(nextRefundedCost);
        }

        BigDecimal totalRefundedCogs = order.getItems().stream()
                .map(OrderItemEntity::getRefundedCostAmount)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        order.applyRefundedCostOfGoods(totalRefundedCogs);
    }

    private BigDecimal consumeMaterialsForOrderItem(
            OrderItemEntity orderItem,
            UserEntity actor,
            UUID orderId,
            OffsetDateTime occurredAt
    ) {
        List<ProductMaterialRecipeEntity> recipes = productMaterialRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(
                orderItem.getProduct().getId()
        );

        BigDecimal totalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (ProductMaterialRecipeEntity recipe : recipes) {
            int quantity = normalizeSupplyQuantity(recipe.getQuantity(), orderItem.getQuantity(), "material recipe");
            if (quantity <= 0) {
                continue;
            }

            MaterialItemEntity material = recipe.getMaterialItem();
            int quantityDelta = SupplyMovementType.CONSUME_OUT.apply(quantity);
            int quantityAfter = material.getQuantityOnHand() + quantityDelta;
            if (quantityAfter < 0) {
                throw new BadRequestException("Insufficient material stock for order fulfillment");
            }

            ConsumptionResult consumption = consumeMaterialLots(material, quantity);
            material.applyMovement(quantityDelta, null);
            materialMovementRepository.save(new MaterialMovementEntity(
                    material,
                    actor,
                    SupplyMovementType.CONSUME_OUT.name(),
                    quantity,
                    quantityDelta,
                    quantityAfter,
                    consumption.averageUnitCost(),
                    "Consumed by order (FIFO)",
                    "ORDER",
                    orderId,
                    occurredAt
            ));
            totalCost = totalCost.add(consumption.totalCost()).setScale(2, RoundingMode.HALF_UP);
        }

        return totalCost;
    }

    private BigDecimal consumePackagingForOrderItem(
            OrderItemEntity orderItem,
            UserEntity actor,
            UUID orderId,
            OffsetDateTime occurredAt
    ) {
        List<ProductPackagingRecipeEntity> recipes = productPackagingRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(
                orderItem.getProduct().getId()
        );

        BigDecimal totalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (ProductPackagingRecipeEntity recipe : recipes) {
            int quantity = normalizeSupplyQuantity(recipe.getQuantity(), orderItem.getQuantity(), "packaging recipe");
            if (quantity <= 0) {
                continue;
            }

            PackagingItemEntity packagingItem = recipe.getPackagingItem();
            int quantityDelta = SupplyMovementType.CONSUME_OUT.apply(quantity);
            int quantityAfter = packagingItem.getQuantityOnHand() + quantityDelta;
            if (quantityAfter < 0) {
                throw new BadRequestException("Insufficient packaging stock for order fulfillment");
            }

            ConsumptionResult consumption = consumePackagingLots(packagingItem, quantity);
            packagingItem.applyMovement(quantityDelta, null);
            packagingMovementRepository.save(new PackagingMovementEntity(
                    packagingItem,
                    actor,
                    SupplyMovementType.CONSUME_OUT.name(),
                    quantity,
                    quantityDelta,
                    quantityAfter,
                    consumption.averageUnitCost(),
                    "Consumed by order (FIFO)",
                    "ORDER",
                    orderId,
                    occurredAt
            ));
            totalCost = totalCost.add(consumption.totalCost()).setScale(2, RoundingMode.HALF_UP);
        }

        return totalCost;
    }

    private int normalizeSupplyQuantity(BigDecimal quantityPerItem, int itemQuantity, String source) {
        BigDecimal totalQuantity = quantityPerItem.multiply(BigDecimal.valueOf(itemQuantity));
        try {
            return totalQuantity.setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (ArithmeticException error) {
            throw new BadRequestException("Unable to normalize " + source + " quantity for inventory movement");
        }
    }

    private ConsumptionResult consumeMaterialLots(MaterialItemEntity material, int quantity) {
        List<MaterialStockLotEntity> lots = materialStockLotRepository
                .findAllByMaterial_IdAndRemainingQuantityGreaterThanOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(material.getId(), 0)
                .stream()
                .sorted(Comparator
                        .comparing((MaterialStockLotEntity lot) -> lot.getExpiryDate() == null ? OffsetDateTime.MAX : lot.getExpiryDate())
                        .thenComparing(MaterialStockLotEntity::getReceivedAt)
                        .thenComparing(MaterialStockLotEntity::getCreatedAt))
                .toList();

        int remaining = quantity;
        BigDecimal totalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (MaterialStockLotEntity lot : lots) {
            if (remaining <= 0) {
                break;
            }
            int consumed = lot.consume(remaining);
            if (consumed <= 0) {
                continue;
            }
            remaining -= consumed;
            BigDecimal lotUnitCost = lot.getUnitCost() != null ? lot.getUnitCost() : material.getLatestUnitCost();
            totalCost = totalCost.add(calculateCost(consumed, lotUnitCost)).setScale(2, RoundingMode.HALF_UP);
        }

        if (remaining > 0) {
            totalCost = totalCost.add(calculateCost(remaining, material.getLatestUnitCost())).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal averageUnitCost = quantity == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalCost.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        return new ConsumptionResult(totalCost, averageUnitCost);
    }

    private ConsumptionResult consumePackagingLots(PackagingItemEntity packagingItem, int quantity) {
        List<PackagingStockLotEntity> lots = packagingStockLotRepository
                .findAllByPackagingItem_IdAndRemainingQuantityGreaterThanOrderByExpiryDateAscReceivedAtAscCreatedAtAsc(packagingItem.getId(), 0)
                .stream()
                .sorted(Comparator
                        .comparing((PackagingStockLotEntity lot) -> lot.getExpiryDate() == null ? OffsetDateTime.MAX : lot.getExpiryDate())
                        .thenComparing(PackagingStockLotEntity::getReceivedAt)
                        .thenComparing(PackagingStockLotEntity::getCreatedAt))
                .toList();

        int remaining = quantity;
        BigDecimal totalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (PackagingStockLotEntity lot : lots) {
            if (remaining <= 0) {
                break;
            }
            int consumed = lot.consume(remaining);
            if (consumed <= 0) {
                continue;
            }
            remaining -= consumed;
            BigDecimal lotUnitCost = lot.getUnitCost() != null ? lot.getUnitCost() : packagingItem.getLatestUnitCost();
            totalCost = totalCost.add(calculateCost(consumed, lotUnitCost)).setScale(2, RoundingMode.HALF_UP);
        }

        if (remaining > 0) {
            totalCost = totalCost.add(calculateCost(remaining, packagingItem.getLatestUnitCost())).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal averageUnitCost = quantity == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalCost.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        return new ConsumptionResult(totalCost, averageUnitCost);
    }

    private BigDecimal calculateCost(int quantity, BigDecimal unitCost) {
        if (unitCost == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return unitCost.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private record ConsumptionResult(
            BigDecimal totalCost,
            BigDecimal averageUnitCost
    ) {
    }
}
