package com.nucosmos.pos.backend.inventory;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.inventory.persistence.InventoryMovementEntity;
import com.nucosmos.pos.backend.inventory.persistence.InventoryStockEntity;
import com.nucosmos.pos.backend.inventory.repository.InventoryMovementRepository;
import com.nucosmos.pos.backend.inventory.repository.InventoryStockRepository;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.repository.ProductRepository;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    public InventoryService(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            InventoryStockRepository inventoryStockRepository,
            InventoryMovementRepository inventoryMovementRepository
    ) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryStockResponse> listStocks(AuthenticatedUser user, boolean lowStockOnly) {
        StoreEntity store = loadStore(user.storeCode());
        Map<UUID, InventoryStockEntity> stockByProductId = inventoryStockRepository
                .findAllByStore_IdOrderByProduct_Category_DisplayOrderAscProduct_NameAsc(store.getId())
                .stream()
                .collect(Collectors.toMap(stock -> stock.getProduct().getId(), Function.identity()));

        return productRepository.findAllByOrderByCategory_DisplayOrderAscNameAsc()
                .stream()
                .map(product -> toStockResponse(product, stockByProductId.get(product.getId())))
                .filter(stock -> !lowStockOnly || stock.lowStock())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> listMovements(AuthenticatedUser user) {
        StoreEntity store = loadStore(user.storeCode());
        return inventoryMovementRepository.findTop100ByStore_IdOrderByOccurredAtDescCreatedAtDesc(store.getId())
                .stream()
                .map(this::toMovementResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryStockResponse> listDefectiveStocks(AuthenticatedUser user) {
        return listStocks(user, false)
                .stream()
                .filter(stock -> stock.defectiveQuantity() > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> listDefectiveMovements(AuthenticatedUser user) {
        StoreEntity store = loadStore(user.storeCode());
        return inventoryMovementRepository.findTop100ByStore_IdAndStockBucketOrderByOccurredAtDescCreatedAtDesc(
                        store.getId(),
                        InventoryStockBucket.DEFECTIVE.name()
                )
                .stream()
                .map(this::toMovementResponse)
                .toList();
    }

    @Transactional
    public InventoryMovementResponse createMovement(AuthenticatedUser user, InventoryMovementRequest request) {
        StoreEntity store = loadStore(user.storeCode());
        ProductEntity product = loadProduct(request.productId());
        UserEntity createdByUser = userRepository.findById(user.userId())
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));

        InventoryMovementType movementType = parseMovementType(request.movementType());
        InventoryStockEntity stock = inventoryStockRepository.findByStore_IdAndProduct_Id(store.getId(), product.getId())
                .orElseGet(() -> inventoryStockRepository.save(InventoryStockEntity.create(store, product)));

        return saveMovement(
                stock,
                product,
                createdByUser,
                movementType,
                request.quantity(),
                request.unitCost(),
                request.reasonCode(),
                request.note(),
                "MANUAL",
                null
        );
    }

    @Transactional
    public InventoryMovementResponse recordSystemMovement(
            StoreEntity store,
            ProductEntity product,
            UserEntity createdByUser,
            InventoryMovementType movementType,
            int quantity,
            java.math.BigDecimal unitCost,
            String reasonCode,
            String note,
            String referenceType,
            UUID referenceId
    ) {
        InventoryStockEntity stock = inventoryStockRepository.findByStore_IdAndProduct_Id(store.getId(), product.getId())
                .orElseGet(() -> inventoryStockRepository.save(InventoryStockEntity.create(store, product)));

        return saveMovement(
                stock,
                product,
                createdByUser,
                movementType,
                quantity,
                unitCost,
                reasonCode,
                note,
                referenceType,
                referenceId
        );
    }

    @Transactional
    public InventoryMovementResponse scrapDefectiveStock(
            AuthenticatedUser user,
            UUID productId,
            DefectiveInventoryActionRequest request
    ) {
        StoreEntity store = loadStore(user.storeCode());
        ProductEntity product = loadProduct(productId);
        UserEntity createdByUser = userRepository.findById(user.userId())
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));
        InventoryStockEntity stock = inventoryStockRepository.findByStore_IdAndProduct_Id(store.getId(), product.getId())
                .orElseGet(() -> inventoryStockRepository.save(InventoryStockEntity.create(store, product)));

        return saveMovement(
                stock,
                product,
                createdByUser,
                InventoryMovementType.SCRAP_OUT,
                request.quantity(),
                null,
                request.reasonCode(),
                request.note(),
                "DEFECTIVE_ACTION",
                null
        );
    }

    @Transactional
    public InventoryMovementResponse restoreDefectiveStock(
            AuthenticatedUser user,
            UUID productId,
            DefectiveInventoryActionRequest request
    ) {
        StoreEntity store = loadStore(user.storeCode());
        ProductEntity product = loadProduct(productId);
        UserEntity createdByUser = userRepository.findById(user.userId())
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));
        InventoryStockEntity stock = inventoryStockRepository.findByStore_IdAndProduct_Id(store.getId(), product.getId())
                .orElseGet(() -> inventoryStockRepository.save(InventoryStockEntity.create(store, product)));

        return saveMovement(
                stock,
                product,
                createdByUser,
                InventoryMovementType.DEFECTIVE_RESTORE,
                request.quantity(),
                null,
                request.reasonCode(),
                request.note(),
                "DEFECTIVE_ACTION",
                null
        );
    }

    private InventoryMovementResponse saveMovement(
            InventoryStockEntity stock,
            ProductEntity product,
            UserEntity createdByUser,
            InventoryMovementType movementType,
            int quantity,
            java.math.BigDecimal unitCost,
            String reasonCode,
            String note,
            String referenceType,
            UUID referenceId
    ) {
        MovementImpact movementImpact = MovementImpact.from(movementType, quantity);
        int sellableQuantityAfter = stock.getSellableQuantity() + movementImpact.sellableQuantityDelta();
        int defectiveQuantityAfter = stock.getDefectiveQuantity() + movementImpact.defectiveQuantityDelta();
        int quantityAfter = stock.getQuantityOnHand() + movementImpact.totalQuantityDelta();

        if (sellableQuantityAfter < 0 || defectiveQuantityAfter < 0 || quantityAfter < 0) {
            throw new BadRequestException("Insufficient stock for this movement");
        }

        if (movementImpact.sellableQuantityDelta() != 0) {
            stock.applySellableDelta(movementImpact.sellableQuantityDelta());
        }
        if (movementImpact.defectiveQuantityDelta() != 0) {
            stock.applyDefectiveDelta(movementImpact.defectiveQuantityDelta());
        }

        InventoryMovementEntity movement = new InventoryMovementEntity(
                stock.getStore(),
                product,
                createdByUser,
                movementType.name(),
                movementImpact.stockBucket().name(),
                quantity,
                movementImpact.totalQuantityDelta(),
                quantityAfter,
                movementImpact.sellableQuantityDelta(),
                movementImpact.defectiveQuantityDelta(),
                sellableQuantityAfter,
                defectiveQuantityAfter,
                unitCost,
                blankToNull(reasonCode),
                blankToNull(note),
                referenceType,
                referenceId,
                OffsetDateTime.now()
        );

        return toMovementResponse(inventoryMovementRepository.save(movement));
    }

    @Transactional
    public InventoryStockResponse updateReorderLevel(
            AuthenticatedUser user,
            UUID productId,
            InventoryStockLevelUpdateRequest request
    ) {
        StoreEntity store = loadStore(user.storeCode());
        ProductEntity product = loadProduct(productId);

        InventoryStockEntity stock = inventoryStockRepository.findByStore_IdAndProduct_Id(store.getId(), product.getId())
                .orElseGet(() -> inventoryStockRepository.save(InventoryStockEntity.create(store, product)));
        stock.updateReorderLevel(request.reorderLevel());

        return toStockResponse(product, stock);
    }

    private StoreEntity loadStore(String storeCode) {
        return storeRepository.findByCodeAndStatus(storeCode, "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated store is not available"));
    }

    private ProductEntity loadProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BadRequestException("Product not found"));
    }

    private InventoryMovementType parseMovementType(String rawType) {
        try {
            return InventoryMovementType.from(rawType);
        } catch (IllegalArgumentException error) {
            throw new BadRequestException("Inventory movement type is invalid");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private InventoryStockResponse toStockResponse(ProductEntity product, InventoryStockEntity stock) {
        int sellableQuantity = stock == null ? 0 : stock.getSellableQuantity();
        int defectiveQuantity = stock == null ? 0 : stock.getDefectiveQuantity();
        int quantityOnHand = stock == null ? 0 : stock.getQuantityOnHand();
        int reorderLevel = stock == null ? 0 : stock.getReorderLevel();
        return new InventoryStockResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory().getName(),
                product.getImageUrl(),
                sellableQuantity,
                defectiveQuantity,
                quantityOnHand,
                reorderLevel,
                sellableQuantity <= reorderLevel
        );
    }

    private InventoryMovementResponse toMovementResponse(InventoryMovementEntity movement) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getSku(),
                movement.getProduct().getName(),
                movement.getMovementType(),
                movement.getStockBucket(),
                movement.getQuantity(),
                movement.getQuantityDelta(),
                movement.getQuantityAfter(),
                movement.getSellableQuantityDelta(),
                movement.getDefectiveQuantityDelta(),
                movement.getSellableQuantityAfter(),
                movement.getDefectiveQuantityAfter(),
                movement.getUnitCost(),
                movement.getReasonCode(),
                movement.getNote(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getOccurredAt()
        );
    }

    private record MovementImpact(
            InventoryStockBucket stockBucket,
            int sellableQuantityDelta,
            int defectiveQuantityDelta
    ) {
        static MovementImpact from(InventoryMovementType movementType, int quantity) {
            int signedQuantity = movementType.applyDirection(quantity);
            return switch (movementType) {
                case DEFECTIVE_RESTORE -> new MovementImpact(InventoryStockBucket.DEFECTIVE, quantity, -quantity);
                default -> switch (movementType.stockBucket()) {
                    case SELLABLE -> new MovementImpact(InventoryStockBucket.SELLABLE, signedQuantity, 0);
                    case DEFECTIVE -> new MovementImpact(InventoryStockBucket.DEFECTIVE, 0, signedQuantity);
                };
            };
        }

        int totalQuantityDelta() {
            return sellableQuantityDelta + defectiveQuantityDelta;
        }
    }
}
