package com.nucosmos.pos.backend.product;

import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.OffsetDateTime;

@Service
public class ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> listAvailableProducts() {
        return productRepository.findAllByActiveTrueOrderByCategory_DisplayOrderAscNameAsc()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    private ProductSummaryResponse toSummaryResponse(ProductEntity product) {
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
                product.isActive()
        );
    }
}
