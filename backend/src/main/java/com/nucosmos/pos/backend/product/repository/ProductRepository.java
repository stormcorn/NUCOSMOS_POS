package com.nucosmos.pos.backend.product.repository;

import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findAllByActiveTrueOrderByCategory_DisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findAllByActiveTrueAndUpdatedAtAfterOrderByCategory_DisplayOrderAscNameAsc(OffsetDateTime updatedAt);

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findAllByOrderByCategory_DisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findAllByActiveOrderByCategory_DisplayOrderAscNameAsc(boolean active);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findById(UUID uuid);

    @Override
    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findAllById(Iterable<UUID> uuids);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);
}
