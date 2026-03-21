package com.nucosmos.pos.backend.product;

import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.product.persistence.ProductCategoryEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.repository.ProductCategoryRepository;
import com.nucosmos.pos.backend.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductAdminService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public ProductAdminService(
            ProductRepository productRepository,
            ProductCategoryRepository productCategoryRepository
    ) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
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
    public List<ProductAdminResponse> listProducts(Boolean active) {
        List<ProductEntity> products = active == null
                ? productRepository.findAllByOrderByCategory_DisplayOrderAscNameAsc()
                : productRepository.findAllByActiveOrderByCategory_DisplayOrderAscNameAsc(active);

        return products.stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public ProductAdminResponse createProduct(ProductUpsertRequest request) {
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

        return toAdminResponse(productRepository.save(product));
    }

    @Transactional
    public ProductAdminResponse updateProduct(UUID productId, ProductUpsertRequest request) {
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

        return toAdminResponse(product);
    }

    @Transactional
    public ProductAdminResponse deactivateProduct(UUID productId) {
        ProductEntity product = loadProduct(productId);
        product.deactivate();
        return toAdminResponse(product);
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

    private ProductAdminResponse toAdminResponse(ProductEntity product) {
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
                product.isActive()
        );
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
}
