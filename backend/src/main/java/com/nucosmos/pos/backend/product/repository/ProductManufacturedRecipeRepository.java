package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductManufacturedRecipeEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductManufacturedRecipeRepository extends JpaRepository<ProductManufacturedRecipeEntity, UUID> {

    @EntityGraph(attributePaths = {"manufacturedItem"})
    List<ProductManufacturedRecipeEntity> findAllByProduct_IdOrderByCreatedAtAsc(UUID productId);

    void deleteAllByProduct_Id(UUID productId);
}
