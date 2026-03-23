package com.nucosmos.pos.backend.inventory.stocktake;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.inventory.InventoryMovementType;
import com.nucosmos.pos.backend.inventory.InventoryService;
import com.nucosmos.pos.backend.inventory.persistence.InventoryStockEntity;
import com.nucosmos.pos.backend.inventory.repository.InventoryStockRepository;
import com.nucosmos.pos.backend.inventory.stocktake.persistence.InventoryStocktakeEntity;
import com.nucosmos.pos.backend.inventory.stocktake.persistence.InventoryStocktakeItemEntity;
import com.nucosmos.pos.backend.inventory.stocktake.repository.InventoryStocktakeRepository;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.repository.ProductRepository;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryStocktakeService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryStocktakeRepository inventoryStocktakeRepository;
    private final InventoryService inventoryService;

    public InventoryStocktakeService(
            StoreRepository storeRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            InventoryStockRepository inventoryStockRepository,
            InventoryStocktakeRepository inventoryStocktakeRepository,
            InventoryService inventoryService
    ) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryStockRepository = inventoryStockRepository;
        this.inventoryStocktakeRepository = inventoryStocktakeRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public List<InventoryStocktakeResponse> listStocktakes(AuthenticatedUser user) {
        ensureStoreExists(user.storeCode());
        return inventoryStocktakeRepository.findTop20ByStore_CodeOrderByCountedAtDesc(user.storeCode())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryStocktakeResponse createStocktake(AuthenticatedUser user, InventoryStocktakeCreateRequest request) {
        StoreEntity store = ensureStoreExists(user.storeCode());
        UserEntity actor = userRepository.findById(user.userId())
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));

        Map<UUID, InventoryStocktakeCreateItemRequest> requestItemByProductId = new LinkedHashMap<>();
        for (InventoryStocktakeCreateItemRequest item : request.items()) {
            if (requestItemByProductId.putIfAbsent(item.productId(), item) != null) {
                throw new BadRequestException("Stocktake contains duplicate product items");
            }
        }

        List<ProductEntity> products = productRepository.findAllById(requestItemByProductId.keySet());
        Map<UUID, ProductEntity> productById = new LinkedHashMap<>();
        for (ProductEntity product : products) {
            if (!product.isActive()) {
                throw new BadRequestException("Stocktake contains inactive products");
            }
            productById.put(product.getId(), product);
        }

        if (productById.size() != requestItemByProductId.size()) {
            throw new BadRequestException("Stocktake contains unknown products");
        }

        Map<UUID, InventoryStockEntity> stockByProductId = new LinkedHashMap<>();
        for (InventoryStockEntity stock : inventoryStockRepository.findAllByStore_IdOrderByProduct_Category_DisplayOrderAscProduct_NameAsc(store.getId())) {
            stockByProductId.put(stock.getProduct().getId(), stock);
        }

        OffsetDateTime now = OffsetDateTime.now();
        InventoryStocktakeEntity stocktake = InventoryStocktakeEntity.create(store, actor, blankToNull(request.note()), now, now);

        for (InventoryStocktakeCreateItemRequest itemRequest : request.items()) {
            ProductEntity product = productById.get(itemRequest.productId());
            InventoryStockEntity stock = stockByProductId.computeIfAbsent(
                    product.getId(),
                    ignored -> inventoryStockRepository.save(InventoryStockEntity.create(store, product))
            );

            int expectedSellableQuantity = stock.getSellableQuantity();
            int varianceQuantity = itemRequest.countedSellableQuantity() - expectedSellableQuantity;

            InventoryStocktakeItemEntity stocktakeItem = new InventoryStocktakeItemEntity(
                    stocktake,
                    product,
                    expectedSellableQuantity,
                    itemRequest.countedSellableQuantity(),
                    varianceQuantity,
                    blankToNull(itemRequest.reasonCode()),
                    blankToNull(itemRequest.note())
            );
            stocktake.addItem(stocktakeItem);
        }

        InventoryStocktakeEntity savedStocktake = inventoryStocktakeRepository.save(stocktake);

        for (InventoryStocktakeItemEntity item : savedStocktake.getItems()) {
            if (item.getVarianceQuantity() == 0) {
                continue;
            }

            inventoryService.recordSystemMovement(
                    savedStocktake.getStore(),
                    item.getProduct(),
                    savedStocktake.getCreatedByUser(),
                    item.getVarianceQuantity() > 0 ? InventoryMovementType.ADJUSTMENT_IN : InventoryMovementType.ADJUSTMENT_OUT,
                    Math.abs(item.getVarianceQuantity()),
                    null,
                    item.getReasonCode(),
                    item.getNote() != null ? item.getNote() : "Posted from stocktake",
                    "STOCKTAKE",
                    savedStocktake.getId()
            );
        }

        return toResponse(savedStocktake);
    }

    private StoreEntity ensureStoreExists(String storeCode) {
        return storeRepository.findByCodeAndStatus(storeCode, "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated store is not available"));
    }

    private InventoryStocktakeResponse toResponse(InventoryStocktakeEntity stocktake) {
        return new InventoryStocktakeResponse(
                stocktake.getId(),
                stocktake.getStatus(),
                stocktake.getStore().getCode(),
                stocktake.getCreatedByUser().getEmployeeCode(),
                stocktake.getNote(),
                stocktake.getCountedAt(),
                stocktake.getPostedAt(),
                stocktake.getItems().stream()
                        .map(item -> new InventoryStocktakeItemResponse(
                                item.getId(),
                                item.getProduct().getId(),
                                item.getProduct().getSku(),
                                item.getProduct().getName(),
                                item.getProduct().getCategory().getName(),
                                item.getExpectedSellableQuantity(),
                                item.getCountedSellableQuantity(),
                                item.getVarianceQuantity(),
                                item.getReasonCode(),
                                item.getNote()
                        ))
                        .toList()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
