package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductMaterialRecipeEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductMaterialRecipeRepository extends JpaRepository<ProductMaterialRecipeEntity, UUID> {

    @EntityGraph(attributePaths = {"materialItem"})
    List<ProductMaterialRecipeEntity> findAllByProduct_IdOrderByCreatedAtAsc(UUID productId);

    void deleteAllByProduct_Id(UUID productId);
}
