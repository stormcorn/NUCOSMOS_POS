package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionPackagingEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRecipeVersionPackagingRepository extends JpaRepository<ProductRecipeVersionPackagingEntity, UUID> {

    @EntityGraph(attributePaths = {"packagingItem"})
    List<ProductRecipeVersionPackagingEntity> findAllByRecipeVersion_IdOrderByCreatedAtAsc(UUID recipeVersionId);

    void deleteAllByRecipeVersion_Id(UUID recipeVersionId);
}
