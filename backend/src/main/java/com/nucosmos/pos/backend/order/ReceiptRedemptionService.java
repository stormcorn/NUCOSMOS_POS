package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptMemberEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptRedemptionEntity;
import com.nucosmos.pos.backend.order.repository.ReceiptMemberRepository;
import com.nucosmos.pos.backend.order.repository.ReceiptRedemptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class ReceiptRedemptionService {

    private static final char[] CLAIM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CLAIM_CODE_LENGTH = 8;
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final ReceiptRedemptionRepository receiptRedemptionRepository;
    private final ReceiptMemberRepository receiptMemberRepository;
    private final RedeemProperties redeemProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ReceiptRedemptionService(
            ReceiptRedemptionRepository receiptRedemptionRepository,
            ReceiptMemberRepository receiptMemberRepository,
            RedeemProperties redeemProperties
    ) {
        this.receiptRedemptionRepository = receiptRedemptionRepository;
        this.receiptMemberRepository = receiptMemberRepository;
        this.redeemProperties = redeemProperties;
    }

    @Transactional
    public ReceiptRedemptionEntity ensureForOrder(OrderEntity order) {
        ReceiptRedemptionEntity existing = order.getReceiptRedemption();
        if (existing != null) {
            return existing;
        }

        return receiptRedemptionRepository.findByOrder_Id(order.getId())
                .map(found -> {
                    order.setReceiptRedemption(found);
                    return found;
                })
                .orElseGet(() -> {
                    ReceiptRedemptionEntity created = new ReceiptRedemptionEntity(
                            order,
                            generateUniquePublicToken(),
                            generateUniqueClaimCode()
                    );
                    ReceiptRedemptionEntity saved = receiptRedemptionRepository.save(created);
                    order.setReceiptRedemption(saved);
                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public ReceiptRedeemResponse getByToken(String token) {
        ReceiptRedemptionEntity redemption = receiptRedemptionRepository.findByPublicToken(normalizeToken(token))
                .orElseThrow(() -> new NotFoundException("Redeem ticket not found"));
        return toResponse(redemption);
    }

    @Transactional(readOnly = true)
    public ReceiptRedeemResponse getByClaimCode(String claimCode) {
        ReceiptRedemptionEntity redemption = receiptRedemptionRepository.findByClaimCodeIgnoreCase(normalizeClaimCode(claimCode))
                .orElseThrow(() -> new NotFoundException("Redeem ticket not found"));
        return toResponse(redemption);
    }

    @Transactional
    public ReceiptRedeemResponse claimByToken(String token, PublicRedeemClaimRequest request) {
        ReceiptRedemptionEntity redemption = receiptRedemptionRepository.findByPublicToken(normalizeToken(token))
                .orElseThrow(() -> new NotFoundException("Redeem ticket not found"));

        if (!isEligible(redemption.getOrder())) {
            return toResponse(redemption);
        }

        if (!redemption.isClaimed()) {
            ReceiptMemberEntity member = upsertMember(request);
            OffsetDateTime claimedAt = OffsetDateTime.now();
            member.markClaimed(claimedAt);
            redemption.markClaimed(claimedAt, member);
        }

        return toResponse(redemption);
    }

    public String buildRedeemUrl(ReceiptRedemptionEntity redemption) {
        String baseUrl = redeemProperties.getPublicBaseUrl();
        String normalizedBaseUrl = baseUrl == null || baseUrl.isBlank()
                ? "https://nucosmos.io"
                : baseUrl.trim().replaceAll("/+$", "");
        return normalizedBaseUrl + "/redeem/" + redemption.getPublicToken();
    }

    private ReceiptRedeemResponse toResponse(ReceiptRedemptionEntity redemption) {
        OrderEntity order = redemption.getOrder();
        boolean eligible = isEligible(order);
        boolean claimed = redemption.isClaimed();
        boolean claimable = eligible && !claimed;

        String message;
        if (claimed) {
            message = "This receipt has already been redeemed.";
        } else if (!eligible) {
            message = "This receipt is not eligible for redemption.";
        } else {
            message = "This receipt is ready to redeem.";
        }

        return new ReceiptRedeemResponse(
                redemption.getPublicToken(),
                redemption.getClaimCode(),
                buildRedeemUrl(redemption),
                order.getOrderNumber(),
                order.getStore().getCode(),
                order.getStore().getName(),
                order.getItemCount(),
                order.getTotalAmount(),
                order.getPaymentStatus(),
                order.getOrderedAt(),
                redemption.getClaimedAt(),
                eligible,
                claimed,
                claimable,
                message,
                toMemberSummary(redemption.getClaimedMember())
        );
    }

    private ReceiptMemberEntity upsertMember(PublicRedeemClaimRequest request) {
        if (request == null) {
            throw new BadRequestException("Member profile is required");
        }

        String displayName = normalizeDisplayName(request.displayName());
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());

        return receiptMemberRepository.findByPhoneNumber(phoneNumber)
                .map(existing -> {
                    existing.updateProfile(displayName, phoneNumber);
                    return existing;
                })
                .orElseGet(() -> receiptMemberRepository.save(
                        new ReceiptMemberEntity(displayName, phoneNumber, ACTIVE_STATUS)
                ));
    }

    private ReceiptMemberSummary toMemberSummary(ReceiptMemberEntity member) {
        if (member == null) {
            return null;
        }
        return new ReceiptMemberSummary(member.getDisplayName(), member.getPhoneNumber());
    }

    private boolean isEligible(OrderEntity order) {
        String paymentStatus = order.getPaymentStatus();
        String status = order.getStatus();
        if ("VOIDED".equalsIgnoreCase(status)) {
            return false;
        }

        return "PAID".equalsIgnoreCase(paymentStatus)
                || "PARTIALLY_REFUNDED".equalsIgnoreCase(paymentStatus)
                || "REFUNDED".equalsIgnoreCase(paymentStatus);
    }

    private String generateUniquePublicToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (receiptRedemptionRepository.existsByPublicToken(token));
        return token;
    }

    private String generateUniqueClaimCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(CLAIM_CODE_LENGTH);
            for (int index = 0; index < CLAIM_CODE_LENGTH; index++) {
                builder.append(CLAIM_CODE_ALPHABET[secureRandom.nextInt(CLAIM_CODE_ALPHABET.length)]);
            }
            code = builder.toString();
        } while (receiptRedemptionRepository.existsByClaimCodeIgnoreCase(code));
        return code;
    }

    private String normalizeToken(String token) {
        return token == null ? "" : token.trim();
    }

    private String normalizeClaimCode(String claimCode) {
        return claimCode == null ? "" : claimCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Display name is required");
        }
        return value;
    }

    private String normalizePhoneNumber(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.replaceAll("[\\s\\-()]", "").trim();
        if (normalized.matches("^09\\d{8}$")) {
            return "+886" + normalized.substring(1);
        }
        if (normalized.matches("^\\d{10,15}$")) {
            return "+" + normalized;
        }
        if (!normalized.matches("^\\+\\d{10,15}$")) {
            throw new BadRequestException("Phone number format is invalid");
        }
        return normalized;
    }
}
