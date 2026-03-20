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
                .map(category -> new ProductCategorySummaryResponse(
                        category.getId(),
                        category.getCode(),
                        category.getName(),
                        category.getDisplayOrder(),
                        category.isActive()
                ))
                .toList();
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

    private ProductCategoryEntity loadActiveCategory(UUID categoryId) {
        return productCategoryRepository.findByIdAndActiveTrue(categoryId)
                .orElseThrow(() -> new BadRequestException("Product category is invalid or inactive"));
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
                product.getPrice(),
                product.isActive()
        );
    }
}
