package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionMaterialEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRecipeVersionMaterialRepository extends JpaRepository<ProductRecipeVersionMaterialEntity, UUID> {

    @EntityGraph(attributePaths = {"materialItem"})
    List<ProductRecipeVersionMaterialEntity> findAllByRecipeVersion_IdOrderByCreatedAtAsc(UUID recipeVersionId);

    void deleteAllByRecipeVersion_Id(UUID recipeVersionId);
}
