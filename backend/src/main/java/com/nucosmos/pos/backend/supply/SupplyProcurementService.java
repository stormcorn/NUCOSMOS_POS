package com.nucosmos.pos.backend.supply;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import com.nucosmos.pos.backend.supply.persistence.MaterialItemEntity;
import com.nucosmos.pos.backend.supply.persistence.MaterialMovementEntity;
import com.nucosmos.pos.backend.supply.persistence.MaterialStockLotEntity;
import com.nucosmos.pos.backend.supply.persistence.ManufacturedItemEntity;
import com.nucosmos.pos.backend.supply.persistence.ManufacturedMovementEntity;
import com.nucosmos.pos.backend.supply.persistence.ManufacturedStockLotEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingItemEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingMovementEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingStockLotEntity;
import com.nucosmos.pos.backend.supply.persistence.PurchaseOrderEntity;
import com.nucosmos.pos.backend.supply.persistence.PurchaseOrderLineEntity;
import com.nucosmos.pos.backend.supply.persistence.SupplierEntity;
import com.nucosmos.pos.backend.supply.repository.MaterialItemRepository;
import com.nucosmos.pos.backend.supply.repository.MaterialMovementRepository;
import com.nucosmos.pos.backend.supply.repository.MaterialStockLotRepository;
import com.nucosmos.pos.backend.supply.repository.ManufacturedItemRepository;
import com.nucosmos.pos.backend.supply.repository.ManufacturedMovementRepository;
import com.nucosmos.pos.backend.supply.repository.ManufacturedStockLotRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingItemRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingMovementRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingStockLotRepository;
import com.nucosmos.pos.backend.supply.repository.PurchaseOrderRepository;
import com.nucosmos.pos.backend.supply.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class SupplyProcurementService {

    private static final DateTimeFormatter ORDER_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialItemRepository materialItemRepository;
    private final ManufacturedItemRepository manufacturedItemRepository;
    private final PackagingItemRepository packagingItemRepository;
    private final MaterialMovementRepository materialMovementRepository;
    private final ManufacturedMovementRepository manufacturedMovementRepository;
    private final PackagingMovementRepository packagingMovementRepository;
    private final MaterialStockLotRepository materialStockLotRepository;
    private final ManufacturedStockLotRepository manufacturedStockLotRepository;
    private final PackagingStockLotRepository packagingStockLotRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplyProcurementService(
            StoreRepository storeRepository,
            UserRepository userRepository,
            SupplierRepository supplierRepository,
            MaterialItemRepository materialItemRepository,
            ManufacturedItemRepository manufacturedItemRepository,
            PackagingItemRepository packagingItemRepository,
            MaterialMovementRepository materialMovementRepository,
            ManufacturedMovementRepository manufacturedMovementRepository,
            PackagingMovementRepository packagingMovementRepository,
            MaterialStockLotRepository materialStockLotRepository,
            ManufacturedStockLotRepository manufacturedStockLotRepository,
            PackagingStockLotRepository packagingStockLotRepository,
            PurchaseOrderRepository purchaseOrderRepository
    ) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.materialItemRepository = materialItemRepository;
        this.manufacturedItemRepository = manufacturedItemRepository;
        this.packagingItemRepository = packagingItemRepository;
        this.materialMovementRepository = materialMovementRepository;
        this.manufacturedMovementRepository = manufacturedMovementRepository;
        this.packagingMovementRepository = packagingMovementRepository;
        this.materialStockLotRepository = materialStockLotRepository;
        this.manufacturedStockLotRepository = manufacturedStockLotRepository;
        this.packagingStockLotRepository = packagingStockLotRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> listSuppliers(AuthenticatedUser user) {
        ensureStoreExists(user.storeCode());
        return supplierRepository.findAllByStore_CodeOrderByActiveDescNameAsc(user.storeCode())
                .stream()
                .map(this::toSupplierResponse)
                .toList();
    }

    @Transactional
    public SupplierResponse createSupplier(AuthenticatedUser user, SupplierUpsertRequest request) {
        validateUniqueSupplierCode(user.storeCode(), request.code(), null);
        SupplierEntity supplier = SupplierEntity.create(
                ensureStoreExists(user.storeCode()),
                request.code().trim(),
                request.name().trim(),
                request.contactName(),
                request.phone(),
                request.email(),
                request.note()
        );
        return toSupplierResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse updateSupplier(AuthenticatedUser user, UUID supplierId, SupplierUpsertRequest request) {
        SupplierEntity supplier = loadSupplier(user.storeCode(), supplierId);
        validateUniqueSupplierCode(user.storeCode(), request.code(), supplierId);
        supplier.update(
                request.code().trim(),
                request.name().trim(),
                request.contactName(),
                request.phone(),
                request.email(),
                request.note()
        );
        return toSupplierResponse(supplier);
    }

    @Transactional
    public SupplierResponse deactivateSupplier(AuthenticatedUser user, UUID supplierId) {
        SupplierEntity supplier = loadSupplier(user.storeCode(), supplierId);
        supplier.deactivate();
        return toSupplierResponse(supplier);
    }

    @Transactional(readOnly = true)
    public List<ReplenishmentSuggestionResponse> listReplenishmentSuggestions(AuthenticatedUser user) {
        ensureStoreExists(user.storeCode());

        List<ReplenishmentSuggestionResponse> materials = materialItemRepository.findAllByStore_CodeOrderByActiveDescNameAsc(user.storeCode())
                .stream()
                .filter(MaterialItemEntity::isActive)
                .filter(item -> item.getQuantityOnHand() <= item.getReorderLevel())
                .map(item -> toSuggestion(
                        "MATERIAL",
                        item.getId(),
                        item.getSku(),
                        item.getName(),
                        item.getUnit(),
                        item.getPurchaseUnit(),
                        item.getPurchaseToStockRatio(),
                        item.getQuantityOnHand(),
                        item.getReorderLevel(),
                        item.getLatestUnitCost()
                ))
                .toList();

        List<ReplenishmentSuggestionResponse> packaging = packagingItemRepository.findAllByStore_CodeOrderByActiveDescNameAsc(user.storeCode())
                .stream()
                .filter(PackagingItemEntity::isActive)
                .filter(item -> item.getQuantityOnHand() <= item.getReorderLevel())
                .map(item -> toSuggestion(
                        "PACKAGING",
                        item.getId(),
                        item.getSku(),
                        item.getName(),
                        item.getUnit(),
                        item.getPurchaseUnit(),
                        item.getPurchaseToStockRatio(),
                        item.getQuantityOnHand(),
                        item.getReorderLevel(),
                        item.getLatestUnitCost()
                ))
                .toList();

        List<ReplenishmentSuggestionResponse> manufactured = manufacturedItemRepository.findAllByStore_CodeOrderByActiveDescNameAsc(user.storeCode())
                .stream()
                .filter(ManufacturedItemEntity::isActive)
                .filter(item -> item.getQuantityOnHand() <= item.getReorderLevel())
                .map(item -> toSuggestion(
                        "MANUFACTURED",
                        item.getId(),
                        item.getSku(),
                        item.getName(),
                        item.getUnit(),
                        item.getPurchaseUnit(),
                        item.getPurchaseToStockRatio(),
                        item.getQuantityOnHand(),
                        item.getReorderLevel(),
                        item.getLatestUnitCost()
                ))
                .toList();

        return java.util.stream.Stream.of(materials, manufactured, packaging)
                .flatMap(List::stream)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> listPurchaseOrders(AuthenticatedUser user) {
        ensureStoreExists(user.storeCode());
        return purchaseOrderRepository.findTop50ByStore_CodeOrderByCreatedAtDesc(user.storeCode())
                .stream()
                .map(this::toPurchaseOrderResponse)
                .toList();
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(AuthenticatedUser user, PurchaseOrderCreateRequest request) {
        StoreEntity store = ensureStoreExists(user.storeCode());
        UserEntity actor = loadUser(user.userId());
        SupplierEntity supplier = loadSupplier(user.storeCode(), request.supplierId());
        PurchaseOrderEntity purchaseOrder = PurchaseOrderEntity.create(
                store,
                supplier,
                actor,
                generateOrderNumber(store.getCode()),
                request.note(),
                request.expectedAt()
        );

        for (PurchaseOrderLineRequest lineRequest : request.lines()) {
            SupplyItemType itemType = parseItemType(lineRequest.itemType());
            PurchaseOrderLineEntity line = switch (itemType) {
                case MATERIAL -> PurchaseOrderLineEntity.createForMaterial(
                        purchaseOrder,
                        loadMaterial(user.storeCode(), lineRequest.itemId()),
                        lineRequest.orderedQuantity(),
                        lineRequest.unitCost(),
                        lineRequest.batchCode(),
                        lineRequest.expiryDate(),
                        lineRequest.manufacturedAt(),
                        lineRequest.note()
                );
                case MANUFACTURED -> PurchaseOrderLineEntity.createForManufactured(
                        purchaseOrder,
                        loadManufacturedItem(user.storeCode(), lineRequest.itemId()),
                        lineRequest.orderedQuantity(),
                        lineRequest.unitCost(),
                        lineRequest.batchCode(),
                        lineRequest.expiryDate(),
                        lineRequest.manufacturedAt(),
                        lineRequest.note()
                );
                case PACKAGING -> PurchaseOrderLineEntity.createForPackaging(
                        purchaseOrder,
                        loadPackagingItem(user.storeCode(), lineRequest.itemId()),
                        lineRequest.orderedQuantity(),
                        lineRequest.unitCost(),
                        lineRequest.batchCode(),
                        lineRequest.expiryDate(),
                        lineRequest.manufacturedAt(),
                        lineRequest.note()
                );
            };
            purchaseOrder.addLine(line);
        }

        return toPurchaseOrderResponse(purchaseOrderRepository.save(purchaseOrder));
    }

    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(
            AuthenticatedUser user,
            UUID purchaseOrderId,
            PurchaseOrderReceiveRequest request
    ) {
        PurchaseOrderEntity purchaseOrder = purchaseOrderRepository.findByIdAndStore_Code(purchaseOrderId, user.storeCode())
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
        if ("RECEIVED".equals(purchaseOrder.getStatus())) {
            throw new BadRequestException("Purchase order is already received");
        }

        UserEntity actor = loadUser(user.userId());
        OffsetDateTime receivedAt = OffsetDateTime.now();
        for (PurchaseOrderLineEntity line : purchaseOrder.getLines()) {
            if ("MATERIAL".equals(line.getItemType())) {
                MaterialItemEntity materialItem = line.getMaterialItem();
                int stockQuantity = line.getOrderedQuantity() * materialItem.getPurchaseToStockRatio();
                int quantityAfter = materialItem.getQuantityOnHand() + stockQuantity;
                materialItem.applyMovement(stockQuantity, toStockUnitCost(line.getUnitCost(), materialItem.getPurchaseToStockRatio()));
                materialStockLotRepository.save(MaterialStockLotEntity.create(
                        materialItem,
                        "PURCHASE_ORDER",
                        purchaseOrder.getId(),
                        line.getBatchCode(),
                        line.getExpiryDate(),
                        line.getManufacturedAt(),
                        stockQuantity,
                        toStockUnitCost(line.getUnitCost(), materialItem.getPurchaseToStockRatio()),
                        receivedAt
                ));
                materialMovementRepository.save(new MaterialMovementEntity(
                        materialItem,
                        actor,
                        SupplyMovementType.PURCHASE_IN.name(),
                        stockQuantity,
                        stockQuantity,
                        quantityAfter,
                        toStockUnitCost(line.getUnitCost(), materialItem.getPurchaseToStockRatio()),
                        "Received from purchase order",
                        "PURCHASE_ORDER",
                        purchaseOrder.getId(),
                        receivedAt
                ));
            } else if ("MANUFACTURED".equals(line.getItemType())) {
                ManufacturedItemEntity manufacturedItem = line.getManufacturedItem();
                int stockQuantity = line.getOrderedQuantity() * manufacturedItem.getPurchaseToStockRatio();
                int quantityAfter = manufacturedItem.getQuantityOnHand() + stockQuantity;
                manufacturedItem.applyMovement(stockQuantity, toStockUnitCost(line.getUnitCost(), manufacturedItem.getPurchaseToStockRatio()));
                manufacturedStockLotRepository.save(ManufacturedStockLotEntity.create(
                        manufacturedItem,
                        "PURCHASE_ORDER",
                        purchaseOrder.getId(),
                        line.getBatchCode(),
                        line.getExpiryDate(),
                        line.getManufacturedAt(),
                        stockQuantity,
                        toStockUnitCost(line.getUnitCost(), manufacturedItem.getPurchaseToStockRatio()),
                        receivedAt
                ));
                manufacturedMovementRepository.save(new ManufacturedMovementEntity(
                        manufacturedItem,
                        actor,
                        SupplyMovementType.PURCHASE_IN.name(),
                        stockQuantity,
                        stockQuantity,
                        quantityAfter,
                        toStockUnitCost(line.getUnitCost(), manufacturedItem.getPurchaseToStockRatio()),
                        "Received from purchase order",
                        "PURCHASE_ORDER",
                        purchaseOrder.getId(),
                        receivedAt
                ));
            } else {
                PackagingItemEntity packagingItem = line.getPackagingItem();
                int stockQuantity = line.getOrderedQuantity() * packagingItem.getPurchaseToStockRatio();
                int quantityAfter = packagingItem.getQuantityOnHand() + stockQuantity;
                packagingItem.applyMovement(stockQuantity, toStockUnitCost(line.getUnitCost(), packagingItem.getPurchaseToStockRatio()));
                packagingStockLotRepository.save(PackagingStockLotEntity.create(
                        packagingItem,
                        "PURCHASE_ORDER",
                        purchaseOrder.getId(),
                        line.getBatchCode(),
                        line.getExpiryDate(),
                        line.getManufacturedAt(),
                        stockQuantity,
                        toStockUnitCost(line.getUnitCost(), packagingItem.getPurchaseToStockRatio()),
                        receivedAt
                ));
                packagingMovementRepository.save(new PackagingMovementEntity(
                        packagingItem,
                        actor,
                        SupplyMovementType.PURCHASE_IN.name(),
                        stockQuantity,
                        stockQuantity,
                        quantityAfter,
                        toStockUnitCost(line.getUnitCost(), packagingItem.getPurchaseToStockRatio()),
                        "Received from purchase order",
                        "PURCHASE_ORDER",
                        purchaseOrder.getId(),
                        receivedAt
                ));
            }
            line.markReceived();
        }

        purchaseOrder.receive(request.note(), receivedAt);
        return toPurchaseOrderResponse(purchaseOrder);
    }

    private ReplenishmentSuggestionResponse toSuggestion(
            String itemType,
            UUID itemId,
            String sku,
            String name,
            String stockUnit,
            String purchaseUnit,
            int purchaseToStockRatio,
            int quantityOnHand,
            int reorderLevel,
            BigDecimal latestUnitCost
    ) {
        int targetStockQuantity = Math.max((reorderLevel * 2) - quantityOnHand, Math.max(reorderLevel - quantityOnHand, 1));
        int suggestedOrderQuantity = Math.max((int) Math.ceil((double) targetStockQuantity / purchaseToStockRatio), 1);
        BigDecimal latestPurchaseUnitCost = toPurchaseUnitCost(latestUnitCost, purchaseToStockRatio);
        BigDecimal estimatedOrderCost = latestPurchaseUnitCost == null
                ? null
                : latestPurchaseUnitCost.multiply(BigDecimal.valueOf(suggestedOrderQuantity)).setScale(2, RoundingMode.HALF_UP);
        return new ReplenishmentSuggestionResponse(
                itemType,
                itemId,
                sku,
                name,
                stockUnit,
                purchaseUnit,
                purchaseToStockRatio,
                quantityOnHand,
                reorderLevel,
                suggestedOrderQuantity,
                latestUnitCost,
                latestPurchaseUnitCost,
                estimatedOrderCost
        );
    }

    private SupplierResponse toSupplierResponse(SupplierEntity supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getPhone(),
                supplier.getEmail(),
                supplier.getNote(),
                supplier.isActive()
        );
    }

    private PurchaseOrderResponse toPurchaseOrderResponse(PurchaseOrderEntity purchaseOrder) {
        return new PurchaseOrderResponse(
                purchaseOrder.getId(),
                purchaseOrder.getOrderNumber(),
                purchaseOrder.getStatus(),
                purchaseOrder.getStore().getCode(),
                purchaseOrder.getSupplier().getId(),
                purchaseOrder.getSupplier().getCode(),
                purchaseOrder.getSupplier().getName(),
                purchaseOrder.getCreatedByUser().getEmployeeCode(),
                purchaseOrder.getNote(),
                purchaseOrder.getExpectedAt(),
                purchaseOrder.getReceivedAt(),
                purchaseOrder.getLines().stream()
                        .map(line -> new PurchaseOrderLineResponse(
                                line.getId(),
                                line.getItemType(),
                                switch (line.getItemType()) {
                                    case "MATERIAL" -> line.getMaterialItem().getId();
                                    case "MANUFACTURED" -> line.getManufacturedItem().getId();
                                    default -> line.getPackagingItem().getId();
                                },
                                line.getItemSku(),
                                line.getItemName(),
                                line.getUnit(),
                                switch (line.getItemType()) {
                                    case "MATERIAL" -> line.getMaterialItem().getUnit();
                                    case "MANUFACTURED" -> line.getManufacturedItem().getUnit();
                                    default -> line.getPackagingItem().getUnit();
                                },
                                switch (line.getItemType()) {
                                    case "MATERIAL" -> line.getMaterialItem().getPurchaseToStockRatio();
                                    case "MANUFACTURED" -> line.getManufacturedItem().getPurchaseToStockRatio();
                                    default -> line.getPackagingItem().getPurchaseToStockRatio();
                                },
                                line.getOrderedQuantity(),
                                line.getReceivedQuantity(),
                                switch (line.getItemType()) {
                                    case "MATERIAL" -> line.getReceivedQuantity() * line.getMaterialItem().getPurchaseToStockRatio();
                                    case "MANUFACTURED" -> line.getReceivedQuantity() * line.getManufacturedItem().getPurchaseToStockRatio();
                                    default -> line.getReceivedQuantity() * line.getPackagingItem().getPurchaseToStockRatio();
                                },
                                line.getUnitCost(),
                                line.getBatchCode(),
                                line.getExpiryDate(),
                                line.getManufacturedAt(),
                                line.getNote()
                        ))
                        .toList()
        );
    }

    private StoreEntity ensureStoreExists(String storeCode) {
        return storeRepository.findByCodeAndStatus(storeCode, "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated store is not available"));
    }

    private UserEntity loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));
    }

    private SupplierEntity loadSupplier(String storeCode, UUID supplierId) {
        return supplierRepository.findByIdAndStore_Code(supplierId, storeCode)
                .orElseThrow(() -> new NotFoundException("Supplier not found"));
    }

    private MaterialItemEntity loadMaterial(String storeCode, UUID materialId) {
        return materialItemRepository.findByIdAndStore_Code(materialId, storeCode)
                .orElseThrow(() -> new NotFoundException("Material item not found"));
    }

    private ManufacturedItemEntity loadManufacturedItem(String storeCode, UUID manufacturedItemId) {
        return manufacturedItemRepository.findByIdAndStore_Code(manufacturedItemId, storeCode)
                .orElseThrow(() -> new NotFoundException("Manufactured item not found"));
    }

    private PackagingItemEntity loadPackagingItem(String storeCode, UUID packagingItemId) {
        return packagingItemRepository.findByIdAndStore_Code(packagingItemId, storeCode)
                .orElseThrow(() -> new NotFoundException("Packaging item not found"));
    }

    private void validateUniqueSupplierCode(String storeCode, String rawCode, UUID excludeId) {
        String code = rawCode.trim();
        boolean exists = excludeId == null
                ? supplierRepository.existsByStore_CodeAndCodeIgnoreCase(storeCode, code)
                : supplierRepository.existsByStore_CodeAndCodeIgnoreCaseAndIdNot(storeCode, code, excludeId);
        if (exists) {
            throw new BadRequestException("Supplier code already exists");
        }
    }

    private SupplyItemType parseItemType(String rawType) {
        try {
            return SupplyItemType.from(rawType);
        } catch (IllegalArgumentException error) {
            throw new BadRequestException("Supply item type is invalid");
        }
    }

    private String generateOrderNumber(String storeCode) {
        return "PO-" + storeCode + "-" + OffsetDateTime.now().format(ORDER_NUMBER_FORMAT) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private BigDecimal toPurchaseUnitCost(BigDecimal latestUnitCost, int purchaseToStockRatio) {
        if (latestUnitCost == null) {
            return null;
        }

        return latestUnitCost.multiply(BigDecimal.valueOf(purchaseToStockRatio)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toStockUnitCost(BigDecimal purchaseUnitCost, int purchaseToStockRatio) {
        if (purchaseUnitCost == null) {
            return null;
        }

        return purchaseUnitCost.divide(BigDecimal.valueOf(purchaseToStockRatio), 6, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
