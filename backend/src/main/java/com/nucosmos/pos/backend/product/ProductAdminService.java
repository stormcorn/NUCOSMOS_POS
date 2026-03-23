package com.nucosmos.pos.backend.product;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.product.persistence.ProductCategoryEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.persistence.ProductMaterialRecipeEntity;
import com.nucosmos.pos.backend.product.persistence.ProductPackagingRecipeEntity;
import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionEntity;
import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionMaterialEntity;
import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionPackagingEntity;
import com.nucosmos.pos.backend.product.repository.ProductCategoryRepository;
import com.nucosmos.pos.backend.product.repository.ProductMaterialRecipeRepository;
import com.nucosmos.pos.backend.product.repository.ProductPackagingRecipeRepository;
import com.nucosmos.pos.backend.product.repository.ProductRecipeVersionMaterialRepository;
import com.nucosmos.pos.backend.product.repository.ProductRecipeVersionPackagingRepository;
import com.nucosmos.pos.backend.product.repository.ProductRecipeVersionRepository;
import com.nucosmos.pos.backend.product.repository.ProductRepository;
import com.nucosmos.pos.backend.supply.persistence.MaterialItemEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingItemEntity;
import com.nucosmos.pos.backend.supply.repository.MaterialItemRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductAdminService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductMaterialRecipeRepository productMaterialRecipeRepository;
    private final ProductPackagingRecipeRepository productPackagingRecipeRepository;
    private final ProductRecipeVersionRepository productRecipeVersionRepository;
    private final ProductRecipeVersionMaterialRepository productRecipeVersionMaterialRepository;
    private final ProductRecipeVersionPackagingRepository productRecipeVersionPackagingRepository;
    private final MaterialItemRepository materialItemRepository;
    private final PackagingItemRepository packagingItemRepository;

    public ProductAdminService(
            ProductRepository productRepository,
            ProductCategoryRepository productCategoryRepository,
            ProductMaterialRecipeRepository productMaterialRecipeRepository,
            ProductPackagingRecipeRepository productPackagingRecipeRepository,
            ProductRecipeVersionRepository productRecipeVersionRepository,
            ProductRecipeVersionMaterialRepository productRecipeVersionMaterialRepository,
            ProductRecipeVersionPackagingRepository productRecipeVersionPackagingRepository,
            MaterialItemRepository materialItemRepository,
            PackagingItemRepository packagingItemRepository
    ) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productMaterialRecipeRepository = productMaterialRecipeRepository;
        this.productPackagingRecipeRepository = productPackagingRecipeRepository;
        this.productRecipeVersionRepository = productRecipeVersionRepository;
        this.productRecipeVersionMaterialRepository = productRecipeVersionMaterialRepository;
        this.productRecipeVersionPackagingRepository = productRecipeVersionPackagingRepository;
        this.materialItemRepository = materialItemRepository;
        this.packagingItemRepository = packagingItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductCategorySummaryResponse> listProductCategories() {
        return productCategoryRepository.findAllByOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(this::toCategorySummary)
                .toList();
    }

    @Transactional
    public ProductCategorySummaryResponse createProductCategory(ProductCategoryUpsertRequest request) {
        validateUniqueCategoryCode(request.code(), null);
        ProductCategoryEntity category = ProductCategoryEntity.create(
                request.code().trim(),
                request.name().trim(),
                request.displayOrder()
        );
        return toCategorySummary(productCategoryRepository.save(category));
    }

    @Transactional
    public ProductCategorySummaryResponse updateProductCategory(UUID categoryId, ProductCategoryUpsertRequest request) {
        ProductCategoryEntity category = loadCategory(categoryId);
        validateUniqueCategoryCode(request.code(), categoryId);
        category.update(
                request.code().trim(),
                request.name().trim(),
                request.displayOrder()
        );
        return toCategorySummary(category);
    }

    @Transactional
    public ProductCategorySummaryResponse deactivateProductCategory(UUID categoryId) {
        ProductCategoryEntity category = loadCategory(categoryId);
        if (productRepository.existsByCategory_IdAndActiveTrue(categoryId)) {
            throw new BadRequestException("Product category still has active products");
        }
        category.deactivate();
        return toCategorySummary(category);
    }

    @Transactional(readOnly = true)
    public List<ProductAdminResponse> listProducts(AuthenticatedUser user, Boolean active) {
        List<ProductEntity> products = active == null
                ? productRepository.findAllByOrderByCategory_DisplayOrderAscNameAsc()
                : productRepository.findAllByActiveOrderByCategory_DisplayOrderAscNameAsc(active);

        return products.stream()
                .map(product -> toAdminResponse(product, user.storeCode()))
                .toList();
    }

    @Transactional
    public ProductAdminResponse createProduct(AuthenticatedUser user, ProductUpsertRequest request) {
        validateUniqueSku(request.sku(), null);
        ProductCategoryEntity category = loadActiveCategory(request.categoryId());

        ProductEntity product = ProductEntity.create(
                category,
                request.sku().trim(),
                request.name().trim(),
                request.description(),
                request.imageUrl(),
                request.price()
        );

        ProductEntity savedProduct = productRepository.save(product);
        syncRecipeComponents(savedProduct, user.storeCode(), request, true);
        return toAdminResponse(savedProduct, user.storeCode());
    }

    @Transactional
    public ProductAdminResponse updateProduct(AuthenticatedUser user, UUID productId, ProductUpsertRequest request) {
        ProductEntity product = loadProduct(productId);
        validateUniqueSku(request.sku(), productId);
        ProductCategoryEntity category = loadActiveCategory(request.categoryId());

        product.update(
                category,
                request.sku().trim(),
                request.name().trim(),
                request.description(),
                request.imageUrl(),
                request.price()
        );

        syncRecipeComponents(product, user.storeCode(), request, false);
        return toAdminResponse(product, user.storeCode());
    }

    @Transactional
    public ProductAdminResponse deactivateProduct(UUID productId) {
        ProductEntity product = loadProduct(productId);
        product.deactivate();
        return toAdminResponse(product, null);
    }

    private ProductEntity loadProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private ProductCategoryEntity loadCategory(UUID categoryId) {
        return productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Product category not found"));
    }

    private ProductCategoryEntity loadActiveCategory(UUID categoryId) {
        return productCategoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> new BadRequestException("Product category is invalid or inactive"));
    }

    private void validateUniqueCategoryCode(String rawCode, UUID excludeCategoryId) {
        String code = rawCode.trim();
        boolean exists = excludeCategoryId == null
                ? productCategoryRepository.existsByCodeIgnoreCase(code)
                : productCategoryRepository.existsByCodeIgnoreCaseAndIdNot(code, excludeCategoryId);

        if (exists) {
            throw new BadRequestException("Product category code already exists");
        }
    }

    private void validateUniqueSku(String rawSku, UUID excludeProductId) {
        String sku = rawSku.trim();
        boolean exists = excludeProductId == null
                ? productRepository.existsBySkuIgnoreCase(sku)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, excludeProductId);

        if (exists) {
            throw new BadRequestException("Product SKU already exists");
        }
    }

    private void syncRecipeComponents(ProductEntity product, String storeCode, ProductUpsertRequest request, boolean forceVersion) {
        List<ProductMaterialComponentRequest> materialComponents = request.materialComponents() == null
                ? List.of()
                : request.materialComponents();
        List<ProductPackagingComponentRequest> packagingComponents = request.packagingComponents() == null
                ? List.of()
                : request.packagingComponents();

        validateMaterialComponents(materialComponents);
        validatePackagingComponents(packagingComponents);

        List<ResolvedMaterialComponent> resolvedMaterials = materialComponents.stream()
                .map(component -> new ResolvedMaterialComponent(
                        loadActiveMaterial(component.materialItemId(), storeCode),
                        component.quantity().setScale(3, RoundingMode.HALF_UP)
                ))
                .toList();
        List<ResolvedPackagingComponent> resolvedPackaging = packagingComponents.stream()
                .map(component -> new ResolvedPackagingComponent(
                        loadActivePackaging(component.packagingItemId(), storeCode),
                        component.quantity().setScale(3, RoundingMode.HALF_UP)
                ))
                .toList();

        boolean recipeChanged = forceVersion || hasRecipeChanged(product.getId(), resolvedMaterials, resolvedPackaging);

        if (!recipeChanged) {
            return;
        }

        bootstrapLegacyRecipeVersionIfMissing(product);
        archiveRecipeVersions(product.getId());

        productMaterialRecipeRepository.deleteAllByProduct_Id(product.getId());
        productPackagingRecipeRepository.deleteAllByProduct_Id(product.getId());

        for (ResolvedMaterialComponent component : resolvedMaterials) {
            productMaterialRecipeRepository.save(ProductMaterialRecipeEntity.create(
                    product,
                    component.materialItem(),
                    component.quantity()
            ));
        }

        for (ResolvedPackagingComponent component : resolvedPackaging) {
            productPackagingRecipeRepository.save(ProductPackagingRecipeEntity.create(
                    product,
                    component.packagingItem(),
                    component.quantity()
            ));
        }

        ProductRecipeVersionEntity version = productRecipeVersionRepository.save(ProductRecipeVersionEntity.create(
                product,
                nextRecipeVersionNumber(product.getId()),
                request.recipeNote(),
                OffsetDateTime.now()
        ));

        for (ResolvedMaterialComponent component : resolvedMaterials) {
            productRecipeVersionMaterialRepository.save(ProductRecipeVersionMaterialEntity.create(
                    version,
                    component.materialItem(),
                    component.quantity()
            ));
        }

        for (ResolvedPackagingComponent component : resolvedPackaging) {
            productRecipeVersionPackagingRepository.save(ProductRecipeVersionPackagingEntity.create(
                    version,
                    component.packagingItem(),
                    component.quantity()
            ));
        }
    }

    private void validateMaterialComponents(List<ProductMaterialComponentRequest> components) {
        Set<UUID> seenIds = new HashSet<>();
        for (ProductMaterialComponentRequest component : components) {
            if (!seenIds.add(component.materialItemId())) {
                throw new BadRequestException("Duplicate material component is not allowed");
            }
        }
    }

    private void validatePackagingComponents(List<ProductPackagingComponentRequest> components) {
        Set<UUID> seenIds = new HashSet<>();
        for (ProductPackagingComponentRequest component : components) {
            if (!seenIds.add(component.packagingItemId())) {
                throw new BadRequestException("Duplicate packaging component is not allowed");
            }
        }
    }

    private MaterialItemEntity loadActiveMaterial(UUID materialItemId, String storeCode) {
        MaterialItemEntity materialItem = materialItemRepository.findByIdAndStore_Code(materialItemId, storeCode)
                .orElseThrow(() -> new BadRequestException("Material item is invalid for current store"));

        if (!materialItem.isActive()) {
            throw new BadRequestException("Material item is inactive");
        }

        return materialItem;
    }

    private PackagingItemEntity loadActivePackaging(UUID packagingItemId, String storeCode) {
        PackagingItemEntity packagingItem = packagingItemRepository.findByIdAndStore_Code(packagingItemId, storeCode)
                .orElseThrow(() -> new BadRequestException("Packaging item is invalid for current store"));

        if (!packagingItem.isActive()) {
            throw new BadRequestException("Packaging item is inactive");
        }

        return packagingItem;
    }

    private ProductAdminResponse toAdminResponse(ProductEntity product, String fallbackStoreCode) {
        List<ProductMaterialRecipeEntity> materialRecipes = productMaterialRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(product.getId());
        List<ProductPackagingRecipeEntity> packagingRecipes = productPackagingRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(product.getId());

        List<ProductMaterialComponentResponse> materialComponents = materialRecipes.stream()
                .map(this::toMaterialComponentResponse)
                .toList();
        List<ProductPackagingComponentResponse> packagingComponents = packagingRecipes.stream()
                .map(this::toPackagingComponentResponse)
                .toList();

        BigDecimal materialCost = materialComponents.stream()
                .map(ProductMaterialComponentResponse::lineCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal packagingCost = packagingComponents.stream()
                .map(ProductPackagingComponentResponse::lineCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCost = materialCost.add(packagingCost).setScale(2, RoundingMode.HALF_UP);
        List<ProductRecipeVersionSummaryResponse> recipeVersions = toRecipeVersionSummaries(product, materialRecipes, packagingRecipes);

        return new ProductAdminResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getCode(),
                product.getCategory().getName(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice(),
                product.isActive(),
                materialComponents,
                packagingComponents,
                recipeVersions,
                materialCost,
                packagingCost,
                totalCost
        );
    }

    private ProductMaterialComponentResponse toMaterialComponentResponse(ProductMaterialRecipeEntity recipe) {
        MaterialItemEntity materialItem = recipe.getMaterialItem();
        BigDecimal unitCost = materialItem.getLatestUnitCost();
        BigDecimal lineCost = calculateLineCost(recipe.getQuantity(), unitCost);

        return new ProductMaterialComponentResponse(
                materialItem.getId(),
                materialItem.getSku(),
                materialItem.getName(),
                materialItem.getUnit(),
                recipe.getQuantity(),
                unitCost,
                lineCost
        );
    }

    private ProductPackagingComponentResponse toPackagingComponentResponse(ProductPackagingRecipeEntity recipe) {
        PackagingItemEntity packagingItem = recipe.getPackagingItem();
        BigDecimal unitCost = packagingItem.getLatestUnitCost();
        BigDecimal lineCost = calculateLineCost(recipe.getQuantity(), unitCost);

        return new ProductPackagingComponentResponse(
                packagingItem.getId(),
                packagingItem.getSku(),
                packagingItem.getName(),
                packagingItem.getUnit(),
                packagingItem.getSpecification(),
                recipe.getQuantity(),
                unitCost,
                lineCost
        );
    }

    private BigDecimal calculateLineCost(BigDecimal quantity, BigDecimal unitCost) {
        if (unitCost == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return quantity.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
    }

    private ProductCategorySummaryResponse toCategorySummary(ProductCategoryEntity category) {
        return new ProductCategorySummaryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDisplayOrder(),
                category.isActive()
        );
    }

    private boolean hasRecipeChanged(
            UUID productId,
            List<ResolvedMaterialComponent> requestedMaterials,
            List<ResolvedPackagingComponent> requestedPackaging
    ) {
        Map<UUID, BigDecimal> currentMaterialMap = productMaterialRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(productId)
                .stream()
                .collect(Collectors.toMap(recipe -> recipe.getMaterialItem().getId(), ProductMaterialRecipeEntity::getQuantity));
        Map<UUID, BigDecimal> currentPackagingMap = productPackagingRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(productId)
                .stream()
                .collect(Collectors.toMap(recipe -> recipe.getPackagingItem().getId(), ProductPackagingRecipeEntity::getQuantity));

        Map<UUID, BigDecimal> requestedMaterialMap = requestedMaterials.stream()
                .collect(Collectors.toMap(component -> component.materialItem().getId(), ResolvedMaterialComponent::quantity));
        Map<UUID, BigDecimal> requestedPackagingMap = requestedPackaging.stream()
                .collect(Collectors.toMap(component -> component.packagingItem().getId(), ResolvedPackagingComponent::quantity));

        return !currentMaterialMap.equals(requestedMaterialMap) || !currentPackagingMap.equals(requestedPackagingMap);
    }

    private void bootstrapLegacyRecipeVersionIfMissing(ProductEntity product) {
        List<ProductRecipeVersionEntity> existingVersions = productRecipeVersionRepository.findAllByProduct_IdOrderByVersionNumberDesc(product.getId());
        if (!existingVersions.isEmpty()) {
            return;
        }

        List<ProductMaterialRecipeEntity> legacyMaterials = productMaterialRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(product.getId());
        List<ProductPackagingRecipeEntity> legacyPackaging = productPackagingRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(product.getId());
        if (legacyMaterials.isEmpty() && legacyPackaging.isEmpty()) {
            return;
        }

        ProductRecipeVersionEntity legacyVersion = productRecipeVersionRepository.save(ProductRecipeVersionEntity.create(
                product,
                1,
                "Legacy snapshot before versioned recipe management",
                product.getUpdatedAt()
        ));
        legacyVersion.archive();

        for (ProductMaterialRecipeEntity recipe : legacyMaterials) {
            productRecipeVersionMaterialRepository.save(ProductRecipeVersionMaterialEntity.create(
                    legacyVersion,
                    recipe.getMaterialItem(),
                    recipe.getQuantity()
            ));
        }

        for (ProductPackagingRecipeEntity recipe : legacyPackaging) {
            productRecipeVersionPackagingRepository.save(ProductRecipeVersionPackagingEntity.create(
                    legacyVersion,
                    recipe.getPackagingItem(),
                    recipe.getQuantity()
            ));
        }
    }

    private void archiveRecipeVersions(UUID productId) {
        for (ProductRecipeVersionEntity version : productRecipeVersionRepository.findAllByProduct_IdOrderByVersionNumberDesc(productId)) {
            if ("ACTIVE".equals(version.getStatus())) {
                version.archive();
            }
        }
    }

    private int nextRecipeVersionNumber(UUID productId) {
        return productRecipeVersionRepository.findAllByProduct_IdOrderByVersionNumberDesc(productId)
                .stream()
                .map(ProductRecipeVersionEntity::getVersionNumber)
                .findFirst()
                .orElse(0) + 1;
    }

    private List<ProductRecipeVersionSummaryResponse> toRecipeVersionSummaries(
            ProductEntity product,
            List<ProductMaterialRecipeEntity> currentMaterialRecipes,
            List<ProductPackagingRecipeEntity> currentPackagingRecipes
    ) {
        List<ProductRecipeVersionEntity> versions = productRecipeVersionRepository.findAllByProduct_IdOrderByVersionNumberDesc(product.getId());
        if (versions.isEmpty() && (!currentMaterialRecipes.isEmpty() || !currentPackagingRecipes.isEmpty())) {
            BigDecimal materialCost = currentMaterialRecipes.stream()
                    .map(this::toMaterialComponentResponse)
                    .map(ProductMaterialComponentResponse::lineCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal packagingCost = currentPackagingRecipes.stream()
                    .map(this::toPackagingComponentResponse)
                    .map(ProductPackagingComponentResponse::lineCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            return List.of(new ProductRecipeVersionSummaryResponse(
                    null,
                    1,
                    "ACTIVE",
                    "Legacy current recipe",
                    product.getUpdatedAt(),
                    currentMaterialRecipes.size(),
                    currentPackagingRecipes.size(),
                    materialCost,
                    packagingCost,
                    materialCost.add(packagingCost).setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return versions.stream()
                .map(this::toRecipeVersionSummary)
                .toList();
    }

    private ProductRecipeVersionSummaryResponse toRecipeVersionSummary(ProductRecipeVersionEntity version) {
        List<ProductRecipeVersionMaterialEntity> materialComponents = productRecipeVersionMaterialRepository.findAllByRecipeVersion_IdOrderByCreatedAtAsc(version.getId());
        List<ProductRecipeVersionPackagingEntity> packagingComponents = productRecipeVersionPackagingRepository.findAllByRecipeVersion_IdOrderByCreatedAtAsc(version.getId());

        BigDecimal materialCost = materialComponents.stream()
                .map(component -> calculateLineCost(component.getQuantity(), component.getMaterialItem().getLatestUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal packagingCost = packagingComponents.stream()
                .map(component -> calculateLineCost(component.getQuantity(), component.getPackagingItem().getLatestUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new ProductRecipeVersionSummaryResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getNote(),
                version.getEffectiveAt(),
                materialComponents.size(),
                packagingComponents.size(),
                materialCost,
                packagingCost,
                materialCost.add(packagingCost).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private record ResolvedMaterialComponent(MaterialItemEntity materialItem, BigDecimal quantity) {
    }

    private record ResolvedPackagingComponent(PackagingItemEntity packagingItem, BigDecimal quantity) {
    }
}
