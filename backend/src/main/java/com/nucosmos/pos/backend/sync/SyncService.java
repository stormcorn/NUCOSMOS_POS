package com.nucosmos.pos.backend.sync;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.UnauthorizedException;
import com.nucosmos.pos.backend.device.persistence.DeviceEntity;
import com.nucosmos.pos.backend.device.repository.DeviceRepository;
import com.nucosmos.pos.backend.product.persistence.ProductCategoryEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.repository.ProductCategoryRepository;
import com.nucosmos.pos.backend.product.repository.ProductRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SyncService {

    private final DeviceRepository deviceRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;

    public SyncService(
            DeviceRepository deviceRepository,
            ProductCategoryRepository productCategoryRepository,
            ProductRepository productRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public SyncBootstrapResponse bootstrap(Authentication authentication) {
        AuthenticatedUser user = requireAuthenticatedUser(authentication);
        DeviceEntity device = resolveActiveDevice(user);
        OffsetDateTime now = OffsetDateTime.now();

        return new SyncBootstrapResponse(
                user.storeCode(),
                device.getDeviceCode(),
                device.getStatus(),
                true,
                now,
                productCategoryRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc()
                        .stream()
                        .map(this::toCategoryResponse)
                        .toList(),
                productRepository.findAllByActiveTrueOrderByCategory_DisplayOrderAscNameAsc()
                        .stream()
                        .map(this::toProductResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public SyncCatalogResponse catalog(Authentication authentication, OffsetDateTime since) {
        AuthenticatedUser user = requireAuthenticatedUser(authentication);
        DeviceEntity device = resolveActiveDevice(user);
        OffsetDateTime now = OffsetDateTime.now();

        return new SyncCatalogResponse(
                user.storeCode(),
                device.getDeviceCode(),
                since,
                now,
                productCategoryRepository.findAllByActiveTrueAndUpdatedAtAfterOrderByDisplayOrderAscNameAsc(since)
                        .stream()
                        .map(this::toCategoryResponse)
                        .toList(),
                productRepository.findAllByActiveTrueAndUpdatedAtAfterOrderByCategory_DisplayOrderAscNameAsc(since)
                        .stream()
                        .map(this::toProductResponse)
                        .toList()
        );
    }

    private AuthenticatedUser requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    private DeviceEntity resolveActiveDevice(AuthenticatedUser user) {
        if (!StringUtils.hasText(user.deviceCode())) {
            throw new BadRequestException("Authenticated device is not available");
        }

        return deviceRepository.findByStore_CodeAndDeviceCodeAndStatus(user.storeCode(), user.deviceCode().trim(), "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated device is not available"));
    }

    private SyncCategoryResponse toCategoryResponse(ProductCategoryEntity category) {
        return new SyncCategoryResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDisplayOrder(),
                category.isActive(),
                category.getUpdatedAt()
        );
    }

    private SyncProductResponse toProductResponse(ProductEntity product) {
        return new SyncProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getCode(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.isActive(),
                product.getUpdatedAt()
        );
    }
}
