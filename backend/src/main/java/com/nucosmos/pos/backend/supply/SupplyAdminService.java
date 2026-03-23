package com.nucosmos.pos.backend.supply;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import com.nucosmos.pos.backend.supply.persistence.MaterialItemEntity;
import com.nucosmos.pos.backend.supply.persistence.MaterialMovementEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingItemEntity;
import com.nucosmos.pos.backend.supply.persistence.PackagingMovementEntity;
import com.nucosmos.pos.backend.supply.repository.MaterialItemRepository;
import com.nucosmos.pos.backend.supply.repository.MaterialMovementRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingItemRepository;
import com.nucosmos.pos.backend.supply.repository.PackagingMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SupplyAdminService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final MaterialItemRepository materialItemRepository;
    private final MaterialMovementRepository materialMovementRepository;
    private final PackagingItemRepository packagingItemRepository;
    private final PackagingMovementRepository packagingMovementRepository;

    public SupplyAdminService(
            StoreRepository storeRepository,
            UserRepository userRepository,
            MaterialItemRepository materialItemRepository,
            MaterialMovementRepository materialMovementRepository,
            PackagingItemRepository packagingItemRepository,
            PackagingMovementRepository packagingMovementRepository
    ) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.materialItemRepository = materialItemRepository;
        this.materialMovementRepository = materialMovementRepository;
        this.packagingItemRepository = packagingItemRepository;
        this.packagingMovementRepository = packagingMovementRepository;
    }

    @Transactional(readOnly = true)
    public List<MaterialAdminResponse> listMaterials(AuthenticatedUser user) {
        ensureStoreExists(user.storeCode());
        return materialItemRepository.findAllByStore_CodeOrderByActiveDescNameAsc(user.storeCode())
                .stream()
                .map(this::toMaterialResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialMovementResponse> listMaterialMovements(AuthenticatedUser user) {
        ensureStoreExists(user.storeCode());
        return materialMovementRepository.findTop100ByMaterial_Store_CodeOrderByOccurredAtDescCreatedAtDesc(user.storeCode())
                .stream()
                .map(this::toMaterialMovementResponse)
                .toList();
    }

    @Transactional
    public MaterialAdminResponse createMaterial(AuthenticatedUser user, MaterialUpsertRequest request) {
        validateUniqueMaterialSku(user.storeCode(), request.sku(), null);
        MaterialItemEntity item = MaterialItemEntity.create(
                loadStore(user.storeCode()),
                request.sku().trim(),
                request.name().trim(),
                request.unit().trim(),
                request.description(),
                request.reorderLevel(),
                request.latestUnitCost()
        );
        return toMaterialResponse(materialItemRepository.save(item));
    }

    @Transactional
    public MaterialAdminResponse updateMaterial(AuthenticatedUser user, UUID materialId, MaterialUpsertRequest request) {
        MaterialItemEntity item = loadMaterial(user.storeCode(), materialId);
        validateUniqueMaterialSku(user.storeCode(), request.sku(), materialId);
        item.update(
                request.sku().trim(),
                request.name().trim(),
                request.unit().trim(),
                request.description(),
                request.reorderLevel(),
                request.latestUnitCost()
        );
        return toMaterialResponse(item);
    }

    @Transactional
    public MaterialAdminResponse deactivateMaterial(AuthenticatedUser user, UUID materialId) {
        MaterialItemEntity item = loadMaterial(user.storeCode(), materialId);
        item.deactivate();
        return toMaterialResponse(item);
    }

    @Transactional
    public MaterialMovementResponse createMaterialMovement(
            AuthenticatedUser user,
            UUID materialId,
            MaterialMovementRequest request
    ) {
        MaterialItemEntity item = loadMaterial(user.storeCode(), materialId);
        UserEntity actor = loadUser(user.userId());
        SupplyMovementType movementType = parseSupplyMovementType(request.movementType());
        int quantityDelta = movementType.apply(request.quantity());
        int quantityAfter = item.getQuantityOnHand() + quantityDelta;
        if (quantityAfter < 0) {
            throw new BadRequestException("Insufficient material stock for this movement");
        }

        item.applyMovement(quantityDelta, request.unitCost());
        MaterialMovementEntity movement = new MaterialMovementEntity(
                item,
                actor,
                movementType.name(),
                request.quantity(),
                quantityDelta,
                quantityAfter,
                request.unitCost(),
                blankToNull(request.note()),
                OffsetDateTime.now()
        );
        return toMaterialMovementResponse(materialMovementRepository.save(movement));
    }

    @Transactional(readOnly = true)
    public List<PackagingAdminResponse> listPackagingItems(AuthenticatedUser user) {
        ensureStoreExists(user.storeCode());
        return packagingItemRepository.findAllByStore_CodeOrderByActiveDescNameAsc(user.storeCode())
                .stream()
                .map(this::toPackagingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PackagingMovementResponse> listPackagingMovements(AuthenticatedUser user) {
        ensureStoreExists(user.storeCode());
        return packagingMovementRepository.findTop100ByPackagingItem_Store_CodeOrderByOccurredAtDescCreatedAtDesc(user.storeCode())
                .stream()
                .map(this::toPackagingMovementResponse)
                .toList();
    }

    @Transactional
    public PackagingAdminResponse createPackagingItem(AuthenticatedUser user, PackagingUpsertRequest request) {
        validateUniquePackagingSku(user.storeCode(), request.sku(), null);
        PackagingItemEntity item = PackagingItemEntity.create(
                loadStore(user.storeCode()),
                request.sku().trim(),
                request.name().trim(),
                request.unit().trim(),
                request.specification(),
                request.description(),
                request.reorderLevel(),
                request.latestUnitCost()
        );
        return toPackagingResponse(packagingItemRepository.save(item));
    }

    @Transactional
    public PackagingAdminResponse updatePackagingItem(
            AuthenticatedUser user,
            UUID packagingItemId,
            PackagingUpsertRequest request
    ) {
        PackagingItemEntity item = loadPackagingItem(user.storeCode(), packagingItemId);
        validateUniquePackagingSku(user.storeCode(), request.sku(), packagingItemId);
        item.update(
                request.sku().trim(),
                request.name().trim(),
                request.unit().trim(),
                request.specification(),
                request.description(),
                request.reorderLevel(),
                request.latestUnitCost()
        );
        return toPackagingResponse(item);
    }

    @Transactional
    public PackagingAdminResponse deactivatePackagingItem(AuthenticatedUser user, UUID packagingItemId) {
        PackagingItemEntity item = loadPackagingItem(user.storeCode(), packagingItemId);
        item.deactivate();
        return toPackagingResponse(item);
    }

    @Transactional
    public PackagingMovementResponse createPackagingMovement(
            AuthenticatedUser user,
            UUID packagingItemId,
            PackagingMovementRequest request
    ) {
        PackagingItemEntity item = loadPackagingItem(user.storeCode(), packagingItemId);
        UserEntity actor = loadUser(user.userId());
        SupplyMovementType movementType = parseSupplyMovementType(request.movementType());
        int quantityDelta = movementType.apply(request.quantity());
        int quantityAfter = item.getQuantityOnHand() + quantityDelta;
        if (quantityAfter < 0) {
            throw new BadRequestException("Insufficient packaging stock for this movement");
        }

        item.applyMovement(quantityDelta, request.unitCost());
        PackagingMovementEntity movement = new PackagingMovementEntity(
                item,
                actor,
                movementType.name(),
                request.quantity(),
                quantityDelta,
                quantityAfter,
                request.unitCost(),
                blankToNull(request.note()),
                OffsetDateTime.now()
        );
        return toPackagingMovementResponse(packagingMovementRepository.save(movement));
    }

    private void ensureStoreExists(String storeCode) {
        storeRepository.findByCodeAndStatus(storeCode, "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated store is not available"));
    }

    private com.nucosmos.pos.backend.store.persistence.StoreEntity loadStore(String storeCode) {
        return storeRepository.findByCodeAndStatus(storeCode, "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated store is not available"));
    }

    private UserEntity loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));
    }

    private MaterialItemEntity loadMaterial(String storeCode, UUID materialId) {
        return materialItemRepository.findByIdAndStore_Code(materialId, storeCode)
                .orElseThrow(() -> new NotFoundException("Material item not found"));
    }

    private PackagingItemEntity loadPackagingItem(String storeCode, UUID packagingItemId) {
        return packagingItemRepository.findByIdAndStore_Code(packagingItemId, storeCode)
                .orElseThrow(() -> new NotFoundException("Packaging item not found"));
    }

    private void validateUniqueMaterialSku(String storeCode, String rawSku, UUID excludeId) {
        String sku = rawSku.trim();
        boolean exists = excludeId == null
                ? materialItemRepository.existsByStore_CodeAndSkuIgnoreCase(storeCode, sku)
                : materialItemRepository.existsByStore_CodeAndSkuIgnoreCaseAndIdNot(storeCode, sku, excludeId);
        if (exists) {
            throw new BadRequestException("Material SKU already exists");
        }
    }

    private void validateUniquePackagingSku(String storeCode, String rawSku, UUID excludeId) {
        String sku = rawSku.trim();
        boolean exists = excludeId == null
                ? packagingItemRepository.existsByStore_CodeAndSkuIgnoreCase(storeCode, sku)
                : packagingItemRepository.existsByStore_CodeAndSkuIgnoreCaseAndIdNot(storeCode, sku, excludeId);
        if (exists) {
            throw new BadRequestException("Packaging SKU already exists");
        }
    }

    private SupplyMovementType parseSupplyMovementType(String rawType) {
        try {
            return SupplyMovementType.from(rawType);
        } catch (IllegalArgumentException error) {
            throw new BadRequestException("Supply movement type is invalid");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private MaterialAdminResponse toMaterialResponse(MaterialItemEntity item) {
        return new MaterialAdminResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getUnit(),
                item.getDescription(),
                item.getQuantityOnHand(),
                item.getReorderLevel(),
                item.getLatestUnitCost(),
                item.getQuantityOnHand() <= item.getReorderLevel(),
                item.isActive()
        );
    }

    private MaterialMovementResponse toMaterialMovementResponse(MaterialMovementEntity movement) {
        return new MaterialMovementResponse(
                movement.getId(),
                movement.getMaterial().getId(),
                movement.getMaterial().getSku(),
                movement.getMaterial().getName(),
                movement.getMaterial().getUnit(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getQuantityDelta(),
                movement.getQuantityAfter(),
                movement.getUnitCost(),
                movement.getNote(),
                movement.getOccurredAt()
        );
    }

    private PackagingAdminResponse toPackagingResponse(PackagingItemEntity item) {
        return new PackagingAdminResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getUnit(),
                item.getSpecification(),
                item.getDescription(),
                item.getQuantityOnHand(),
                item.getReorderLevel(),
                item.getLatestUnitCost(),
                item.getQuantityOnHand() <= item.getReorderLevel(),
                item.isActive()
        );
    }

    private PackagingMovementResponse toPackagingMovementResponse(PackagingMovementEntity movement) {
        return new PackagingMovementResponse(
                movement.getId(),
                movement.getPackagingItem().getId(),
                movement.getPackagingItem().getSku(),
                movement.getPackagingItem().getName(),
                movement.getPackagingItem().getUnit(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getQuantityDelta(),
                movement.getQuantityAfter(),
                movement.getUnitCost(),
                movement.getNote(),
                movement.getOccurredAt()
        );
    }
}
