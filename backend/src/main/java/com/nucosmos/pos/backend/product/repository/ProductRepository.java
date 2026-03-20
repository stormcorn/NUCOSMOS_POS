package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findAllByActiveTrueOrderByCategory_DisplayOrderAscNameAsc();
}
