package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductPackagingRecipeEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductPackagingRecipeRepository extends JpaRepository<ProductPackagingRecipeEntity, UUID> {

    @EntityGraph(attributePaths = {"packagingItem"})
    List<ProductPackagingRecipeEntity> findAllByProduct_IdOrderByCreatedAtAsc(UUID productId);

    void deleteAllByProduct_Id(UUID productId);
}
