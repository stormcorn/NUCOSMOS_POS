package com.nucosmos.pos.backend.product;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.common.media.ImageReferenceValidator;
import com.nucosmos.pos.backend.product.persistence.ProductCategoryEntity;
import com.nucosmos.pos.backend.product.persistence.ProductCustomizationGroupEntity;
import com.nucosmos.pos.backend.product.persistence.ProductCustomizationOptionEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.persistence.ProductMaterialRecipeEntity;
import com.nucosmos.pos.backend.product.persistence.ProductPackagingRecipeEntity;
import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionEntity;
import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionMaterialEntity;
import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionPackagingEntity;
import com.nucosmos.pos.backend.product.repository.ProductCategoryRepository;
import com.nucosmos.pos.backend.product.repository.ProductCustomizationGroupRepository;
import com.nucosmos.pos.backend.product.repository.ProductCustomizationOptionRepository;
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
    private final ProductCustomizationGroupRepository productCustomizationGroupRepository;
    private final ProductCustomizationOptionRepository productCustomizationOptionRepository;
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
            ProductCustomizationGroupRepository productCustomizationGroupRepository,
            ProductCustomizationOptionRepository productCustomizationOptionRepository,
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
        this.productCustomizationGroupRepository = productCustomizationGroupRepository;
        this.productCustomizationOptionRepository = productCustomizationOptionRepository;
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
        validateCampaignSettings(request);

        ProductEntity product = ProductEntity.create(
                category,
                request.sku().trim(),
                request.name().trim(),
                request.description(),
                ImageReferenceValidator.normalize(request.imageUrl()),
                request.price(),
                request.campaignEnabled(),
                request.campaignLabel(),
                request.campaignPrice(),
                request.campaignStartsAt(),
                request.campaignEndsAt()
        );

        ProductEntity savedProduct = productRepository.save(product);
        syncRecipeComponents(savedProduct, user.storeCode(), request, true);
        syncCustomizationGroups(savedProduct, request.customizationGroups());
        return toAdminResponse(savedProduct, user.storeCode());
    }

    @Transactional
    public ProductAdminResponse updateProduct(AuthenticatedUser user, UUID productId, ProductUpsertRequest request) {
        ProductEntity product = loadProduct(productId);
        validateUniqueSku(request.sku(), productId);
        ProductCategoryEntity category = loadActiveCategory(request.categoryId());
        validateCampaignSettings(request);

        product.update(
                category,
                request.sku().trim(),
                request.name().trim(),
                request.description(),
                ImageReferenceValidator.normalize(request.imageUrl()),
                request.price(),
                request.campaignEnabled(),
                request.campaignLabel(),
                request.campaignPrice(),
                request.campaignStartsAt(),
                request.campaignEndsAt()
        );

        syncRecipeComponents(product, user.storeCode(), request, false);
        syncCustomizationGroups(product, request.customizationGroups());
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

    private void validateCampaignSettings(ProductUpsertRequest request) {
        if (!request.campaignEnabled()) {
            return;
        }

        if (request.campaignPrice() == null) {
            throw new BadRequestException("Campaign price is required when campaign is enabled");
        }

        if (request.campaignPrice().compareTo(request.price()) > 0) {
            throw new BadRequestException("Campaign price cannot be greater than regular price");
        }

        if (request.campaignStartsAt() != null
                && request.campaignEndsAt() != null
                && request.campaignStartsAt().isAfter(request.campaignEndsAt())) {
            throw new BadRequestException("Campaign starts at must be before campaign ends at");
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
        validateCustomizationGroups(request.customizationGroups());

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

    private void validateCustomizationGroups(List<ProductCustomizationGroupRequest> groups) {
        if (groups == null) {
            return;
        }

        Set<String> seenNames = new HashSet<>();
        for (ProductCustomizationGroupRequest group : groups) {
            String normalizedName = group.name().trim().toLowerCase();
            if (!seenNames.add(normalizedName)) {
                throw new BadRequestException("Duplicate customization group name is not allowed");
            }

            ProductCustomizationSelectionMode selectionMode;
            try {
                selectionMode = ProductCustomizationSelectionMode.from(group.selectionMode());
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Unsupported customization selection mode");
            }

            if (group.maxSelections() < group.minSelections()) {
                throw new BadRequestException("Customization max selections must be greater than or equal to min selections");
            }

            if (group.required() && group.minSelections() < 1) {
                throw new BadRequestException("Required customization group must select at least one option");
            }

            if (selectionMode == ProductCustomizationSelectionMode.SINGLE && group.maxSelections() > 1) {
                throw new BadRequestException("Single selection customization group cannot allow more than one option");
            }

            Set<String> seenOptionNames = new HashSet<>();
            long defaultSelectedCount = 0;
            for (ProductCustomizationOptionRequest option : group.options()) {
                String normalizedOptionName = option.name().trim().toLowerCase();
                if (!seenOptionNames.add(normalizedOptionName)) {
                    throw new BadRequestException("Duplicate customization option name is not allowed");
                }
                if (option.defaultSelected()) {
                    defaultSelectedCount++;
                }
            }

            if (selectionMode == ProductCustomizationSelectionMode.SINGLE && defaultSelectedCount > 1) {
                throw new BadRequestException("Single selection customization group cannot have multiple default options");
            }
            if (defaultSelectedCount > group.maxSelections()) {
                throw new BadRequestException("Default selected options exceed customization max selections");
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

    private void syncCustomizationGroups(ProductEntity product, List<ProductCustomizationGroupRequest> groups) {
        productCustomizationOptionRepository.deleteAllByCustomizationGroup_Product_Id(product.getId());
        productCustomizationGroupRepository.deleteAllByProduct_Id(product.getId());

        if (groups == null || groups.isEmpty()) {
            return;
        }

        for (ProductCustomizationGroupRequest groupRequest : groups) {
            ProductCustomizationGroupEntity group = productCustomizationGroupRepository.save(
                    ProductCustomizationGroupEntity.create(
                            product,
                            groupRequest.name().trim(),
                            ProductCustomizationSelectionMode.from(groupRequest.selectionMode()),
                            groupRequest.required(),
                            groupRequest.minSelections(),
                            groupRequest.maxSelections(),
                            groupRequest.displayOrder()
                    )
            );

            for (ProductCustomizationOptionRequest optionRequest : groupRequest.options()) {
                productCustomizationOptionRepository.save(
                        ProductCustomizationOptionEntity.create(
                                group,
                                optionRequest.name().trim(),
                                optionRequest.priceDelta().setScale(2, RoundingMode.HALF_UP),
                                optionRequest.defaultSelected(),
                                optionRequest.displayOrder()
                        )
                );
            }
        }
    }

    private ProductAdminResponse toAdminResponse(ProductEntity product, String fallbackStoreCode) {
        OffsetDateTime now = OffsetDateTime.now();
        List<ProductMaterialRecipeEntity> materialRecipes = productMaterialRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(product.getId());
        List<ProductPackagingRecipeEntity> packagingRecipes = productPackagingRecipeRepository.findAllByProduct_IdOrderByCreatedAtAsc(product.getId());
        List<ProductCustomizationGroupResponse> customizationGroups = loadCustomizationGroups(product.getId());

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
                product.isCampaignEnabled(),
                product.isCampaignActive(now),
                product.getCampaignLabel(),
                product.getCampaignPrice(),
                product.getCampaignStartsAt(),
                product.getCampaignEndsAt(),
                product.getDisplayPrice(now),
                product.isActive(),
                materialComponents,
                packagingComponents,
                customizationGroups,
                recipeVersions,
                materialCost,
                packagingCost,
                totalCost
        );
    }

    private List<ProductCustomizationGroupResponse> loadCustomizationGroups(UUID productId) {
        List<ProductCustomizationGroupEntity> groups = productCustomizationGroupRepository
                .findAllByProduct_IdOrderByDisplayOrderAscCreatedAtAsc(productId);
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
