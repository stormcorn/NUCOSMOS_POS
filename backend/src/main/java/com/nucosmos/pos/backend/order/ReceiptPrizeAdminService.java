package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.order.persistence.ReceiptPrizeEntity;
import com.nucosmos.pos.backend.order.repository.ReceiptPrizeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ReceiptPrizeAdminService {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final ReceiptPrizeRepository receiptPrizeRepository;

    public ReceiptPrizeAdminService(ReceiptPrizeRepository receiptPrizeRepository) {
        this.receiptPrizeRepository = receiptPrizeRepository;
    }

    @Transactional(readOnly = true)
    public List<ReceiptPrizeAdminResponse> listPrizes() {
        return receiptPrizeRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReceiptPrizeAdminResponse createPrize(ReceiptPrizeAdminRequest request) {
        validateTotalProbability(null, request);
        ReceiptPrizeEntity entity = new ReceiptPrizeEntity(
                normalizeName(request.name()),
                normalizeDescription(request.description()),
                normalizeProbability(request.probabilityPercent()),
                request.remainingQuantity(),
                request.active(),
                request.displayOrder()
        );
        return toResponse(receiptPrizeRepository.save(entity));
    }

    @Transactional
    public ReceiptPrizeAdminResponse updatePrize(UUID prizeId, ReceiptPrizeAdminRequest request) {
        ReceiptPrizeEntity entity = receiptPrizeRepository.findById(prizeId)
                .orElseThrow(() -> new NotFoundException("Redeem prize not found"));
        validateTotalProbability(prizeId, request);
        entity.update(
                normalizeName(request.name()),
                normalizeDescription(request.description()),
                normalizeProbability(request.probabilityPercent()),
                request.remainingQuantity(),
                request.active(),
                request.displayOrder()
        );
        return toResponse(entity);
    }

    @Transactional
    public ReceiptPrizeAdminResponse deactivatePrize(UUID prizeId) {
        ReceiptPrizeEntity entity = receiptPrizeRepository.findById(prizeId)
                .orElseThrow(() -> new NotFoundException("Redeem prize not found"));
        entity.update(
                entity.getName(),
                entity.getDescription(),
                entity.getProbabilityPercent(),
                entity.getRemainingQuantity(),
                false,
                entity.getDisplayOrder()
        );
        return toResponse(entity);
    }

    private void validateTotalProbability(UUID currentPrizeId, ReceiptPrizeAdminRequest request) {
        if (!request.active()) {
            return;
        }

        BigDecimal probability = normalizeProbability(request.probabilityPercent());
        BigDecimal total = receiptPrizeRepository.findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc().stream()
                .filter(prize -> currentPrizeId == null || !prize.getId().equals(currentPrizeId))
                .map(ReceiptPrizeEntity::getProbabilityPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(probability);

        if (total.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("Active prize probabilities cannot exceed 100% in total");
        }
    }

    private ReceiptPrizeAdminResponse toResponse(ReceiptPrizeEntity entity) {
        return new ReceiptPrizeAdminResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getProbabilityPercent(),
                entity.getRemainingQuantity(),
                entity.isActive(),
                entity.getDisplayOrder()
        );
    }

    private String normalizeName(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException("Prize name is required");
        }
        return normalized;
    }

    private String normalizeDescription(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal normalizeProbability(BigDecimal value) {
        if (value == null) {
            throw new BadRequestException("Probability is required");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
