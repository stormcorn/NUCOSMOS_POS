package com.nucosmos.pos.backend.product;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.inventory.persistence.InventoryStockEntity;
import com.nucosmos.pos.backend.inventory.repository.InventoryStockRepository;
import com.nucosmos.pos.backend.product.persistence.ProductCustomizationGroupEntity;
import com.nucosmos.pos.backend.product.persistence.ProductCustomizationOptionEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.repository.ProductCustomizationGroupRepository;
import com.nucosmos.pos.backend.product.repository.ProductCustomizationOptionRepository;
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
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductCustomizationGroupRepository productCustomizationGroupRepository;
    private final ProductCustomizationOptionRepository productCustomizationOptionRepository;
    private final StoreRepository storeRepository;
    private final InventoryStockRepository inventoryStockRepository;

    public ProductQueryService(
            ProductRepository productRepository,
            ProductCustomizationGroupRepository productCustomizationGroupRepository,
            ProductCustomizationOptionRepository productCustomizationOptionRepository,
            StoreRepository storeRepository,
            InventoryStockRepository inventoryStockRepository
    ) {
        this.productRepository = productRepository;
        this.productCustomizationGroupRepository = productCustomizationGroupRepository;
        this.productCustomizationOptionRepository = productCustomizationOptionRepository;
        this.storeRepository = storeRepository;
        this.inventoryStockRepository = inventoryStockRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> listAvailableProducts(AuthenticatedUser user) {
        StoreEntity store = storeRepository.findByCodeAndStatus(user.storeCode(), "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Store is not available"));
        Map<UUID, InventoryStockEntity> stockByProductId = inventoryStockRepository
                .findAllByStore_IdOrderByProduct_Category_DisplayOrderAscProduct_NameAsc(store.getId())
                .stream()
                .collect(Collectors.toMap(stock -> stock.getProduct().getId(), Function.identity()));

        return productRepository.findAllByActiveTrueOrderByCategory_DisplayOrderAscNameAsc()
                .stream()
                .map(product -> toSummaryResponse(product, stockByProductId.get(product.getId())))
                .toList();
    }

    private ProductSummaryResponse toSummaryResponse(ProductEntity product, InventoryStockEntity stock) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ProductSummaryResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getCategory().getCode(),
                product.getCategory().getName(),
                product.getPrice(),
                product.getDisplayPrice(now),
                product.isCampaignEnabled(),
                product.isCampaignActive(now),
                product.getCampaignLabel(),
                product.getCampaignPrice(),
                product.getCampaignStartsAt(),
                product.getCampaignEndsAt(),
                product.isActive(),
                stock == null ? 0 : stock.getSellableQuantity(),
                loadCustomizationGroups(product.getId())
        );
    }

    private List<ProductCustomizationGroupResponse> loadCustomizationGroups(UUID productId) {
        List<ProductCustomizationGroupEntity> groups = productCustomizationGroupRepository
                .findAllByProduct_IdAndActiveTrueOrderByDisplayOrderAscCreatedAtAsc(productId);
        if (groups.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<ProductCustomizationOptionResponse>> optionsByGroupId = productCustomizationOptionRepository
                .findAllByCustomizationGroup_IdInOrderByDisplayOrderAscCreatedAtAsc(groups.stream().map(ProductCustomizationGroupEntity::getId).toList())
                .stream()
                .filter(ProductCustomizationOptionEntity::isActive)
                .collect(Collectors.groupingBy(
                        option -> option.getCustomizationGroup().getId(),
                        Collectors.mapping(
                                option -> new ProductCustomizationOptionResponse(
                                        option.getId(),
                                        option.getName(),
                                        option.getPriceDelta(),
                                        option.isDefaultSelected(),
                                        option.getDisplayOrder(),
                                        option.isActive()
                                ),
                                Collectors.toList()
                        )
                ));

        return groups.stream()
                .map(group -> new ProductCustomizationGroupResponse(
                        group.getId(),
                        group.getName(),
                        group.getSelectionMode().name(),
                        group.isRequired(),
                        group.getMinSelections(),
                        group.getMaxSelections(),
                        group.getDisplayOrder(),
                        group.isActive(),
                        optionsByGroupId.getOrDefault(group.getId(), List.of())
                ))
                .toList();
    }
}
