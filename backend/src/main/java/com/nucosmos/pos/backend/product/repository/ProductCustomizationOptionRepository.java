package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductCustomizationOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductCustomizationOptionRepository extends JpaRepository<ProductCustomizationOptionEntity, UUID> {
    List<ProductCustomizationOptionEntity> findAllByCustomizationGroup_IdInOrderByDisplayOrderAscCreatedAtAsc(Collection<UUID> customizationGroupIds);

    List<ProductCustomizationOptionEntity> findAllByCustomizationGroup_IdOrderByDisplayOrderAscCreatedAtAsc(UUID customizationGroupId);

    void deleteAllByCustomizationGroup_Product_Id(UUID productId);
}
