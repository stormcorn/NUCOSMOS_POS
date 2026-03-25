package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductRecipeVersionManufacturedEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRecipeVersionManufacturedRepository extends JpaRepository<ProductRecipeVersionManufacturedEntity, UUID> {

    @EntityGraph(attributePaths = {"manufacturedItem"})
    List<ProductRecipeVersionManufacturedEntity> findAllByRecipeVersion_IdOrderByCreatedAtAsc(UUID recipeVersionId);

    void deleteAllByRecipeVersion_Id(UUID recipeVersionId);
}
