package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRecipeVersionRepository extends JpaRepository<ProductRecipeVersionEntity, UUID> {

    @EntityGraph(attributePaths = {"product"})
    List<ProductRecipeVersionEntity> findAllByProduct_IdOrderByVersionNumberDesc(UUID productId);
}
