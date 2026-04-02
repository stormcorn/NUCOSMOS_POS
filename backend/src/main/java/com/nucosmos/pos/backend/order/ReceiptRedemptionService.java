package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptCouponEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptMemberEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptPrizeEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptRedemptionEntity;
import com.nucosmos.pos.backend.order.repository.ReceiptCouponRepository;
import com.nucosmos.pos.backend.order.repository.ReceiptMemberRepository;
import com.nucosmos.pos.backend.order.repository.ReceiptPrizeRepository;
import com.nucosmos.pos.backend.order.repository.ReceiptRedemptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ReceiptRedemptionService {

    private static final char[] CLAIM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final char[] COUPON_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CLAIM_CODE_LENGTH = 8;
    private static final int COUPON_CODE_LENGTH = 10;
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String COUPON_ACTIVE_STATUS = "ACTIVE";
    private static final int POINTS_PER_LOSS = 1;
    private static final int COUPON_POINT_THRESHOLD = 5;
    private static final BigDecimal COUPON_DISCOUNT_AMOUNT = new BigDecimal("50.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final String DRAW_OUTCOME_WIN = "WIN";
    private static final String DRAW_OUTCOME_LOSE = "LOSE";
    private static final String SHARE_COUPON_HINT = "在店內五星好評、分享到任意社群、或分享 LINE 好友後出示給老闆看，可直接獲得 50 元抵用券。";

    private final ReceiptRedemptionRepository receiptRedemptionRepository;
    private final ReceiptMemberRepository receiptMemberRepository;
    private final ReceiptCouponRepository receiptCouponRepository;
    private final ReceiptPrizeRepository receiptPrizeRepository;
    private final RedeemProperties redeemProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ReceiptRedemptionService(
            ReceiptRedemptionRepository receiptRedemptionRepository,
            ReceiptMemberRepository receiptMemberRepository,
            ReceiptCouponRepository receiptCouponRepository,
            ReceiptPrizeRepository receiptPrizeRepository,
            RedeemProperties redeemProperties
    ) {
        this.receiptRedemptionRepository = receiptRedemptionRepository;
        this.receiptMemberRepository = receiptMemberRepository;
        this.receiptCouponRepository = receiptCouponRepository;
        this.receiptPrizeRepository = receiptPrizeRepository;
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
            DrawResult drawResult = drawPrize();
            int awardedPoints = drawResult.won() ? 0 : POINTS_PER_LOSS;

            member.markClaimed(claimedAt, awardedPoints);
            redemption.markClaimed(claimedAt, member, drawResult.outcome(), awardedPoints, drawResult.prize());
            ensureCouponForThreshold(redemption, member, awardedPoints);
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
            message = "這張收據已完成兌換。";
        } else if (!eligible) {
            message = "這張收據目前不符合兌獎資格。";
        } else {
            message = "這張收據可參加本次抽獎活動。";
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
                toMemberSummary(redemption.getClaimedMember()),
                toRewardSummary(redemption),
                toDrawSummary(redemption),
                listPrizeSummaries(),
                SHARE_COUPON_HINT
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
        return new ReceiptMemberSummary(
                member.getDisplayName(),
                member.getPhoneNumber(),
                member.getPointBalance(),
                member.getTotalClaims()
        );
    }

    private ReceiptRewardSummary toRewardSummary(ReceiptRedemptionEntity redemption) {
        ReceiptMemberEntity member = redemption.getClaimedMember();
        if (member == null) {
            return null;
        }

        ReceiptCouponSummary couponSummary = receiptCouponRepository.findBySourceRedemption_Id(redemption.getId())
                .map(this::toCouponSummary)
                .orElse(null);

        String rewardMessage;
        if (couponSummary != null) {
            rewardMessage = "未中獎，但已累積點數並獲得 50 元抵用券。";
        } else if (redemption.getAwardedPoints() > 0) {
            rewardMessage = "未中獎，本次已獲得 1 點。";
        } else if (DRAW_OUTCOME_WIN.equalsIgnoreCase(redemption.getDrawOutcome())) {
            rewardMessage = "恭喜中獎，本次不另外累積點數。";
        } else {
            rewardMessage = "尚未兌換。";
        }

        return new ReceiptRewardSummary(
                redemption.getAwardedPoints(),
                member.getPointBalance(),
                couponSummary,
                rewardMessage,
                COUPON_POINT_THRESHOLD,
                COUPON_DISCOUNT_AMOUNT
        );
    }

    private ReceiptDrawSummary toDrawSummary(ReceiptRedemptionEntity redemption) {
        if (!redemption.isClaimed()) {
            return null;
        }

        if (DRAW_OUTCOME_WIN.equalsIgnoreCase(redemption.getDrawOutcome()) && redemption.getPrize() != null) {
            return new ReceiptDrawSummary(
                    DRAW_OUTCOME_WIN,
                    true,
                    "恭喜您中獎了！",
                    "本次抽中「" + redemption.getPrize().getName() + "」。",
                    toPrizeSummary(redemption.getPrize())
            );
        }

        return new ReceiptDrawSummary(
                DRAW_OUTCOME_LOSE,
                false,
                "銘謝惠顧，再接再厲",
                "本次未中獎，已獲得 1 點；每累積 5 點可獲得 50 元抵用券。",
                null
        );
    }

    private ReceiptCouponSummary toCouponSummary(ReceiptCouponEntity coupon) {
        return new ReceiptCouponSummary(
                coupon.getCouponCode(),
                coupon.getTitle(),
                coupon.getDiscountAmount(),
                coupon.getStatus(),
                coupon.getIssuedAt()
        );
    }

    private List<ReceiptPrizeSummary> listPrizeSummaries() {
        return receiptPrizeRepository.findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::toPrizeSummary)
                .toList();
    }

    private ReceiptPrizeSummary toPrizeSummary(ReceiptPrizeEntity prize) {
        return new ReceiptPrizeSummary(
                prize.getId(),
                prize.getName(),
                prize.getDescription(),
                prize.getProbabilityPercent(),
                prize.getRemainingQuantity(),
                prize.isActive(),
                prize.getDisplayOrder()
        );
    }

    private DrawResult drawPrize() {
        List<ReceiptPrizeEntity> prizes = receiptPrizeRepository.findByActiveTrueOrderByDisplayOrderAscCreatedAtAsc().stream()
                .filter(ReceiptPrizeEntity::canDraw)
                .toList();

        BigDecimal totalProbability = prizes.stream()
                .map(ReceiptPrizeEntity::getProbabilityPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (prizes.isEmpty() || totalProbability.compareTo(BigDecimal.ZERO) <= 0) {
            return DrawResult.lose();
        }

        BigDecimal clampedProbability = totalProbability.min(HUNDRED);
        BigDecimal drawValue = BigDecimal.valueOf(secureRandom.nextDouble())
                .multiply(HUNDRED)
                .setScale(6, RoundingMode.HALF_UP);

        if (drawValue.compareTo(clampedProbability) >= 0) {
            return DrawResult.lose();
        }

        BigDecimal cursor = BigDecimal.ZERO;
        for (ReceiptPrizeEntity prize : prizes) {
            cursor = cursor.add(prize.getProbabilityPercent());
            if (drawValue.compareTo(cursor) < 0) {
                prize.decrementQuantity();
                return DrawResult.win(prize);
            }
        }

        return DrawResult.lose();
    }

    private ReceiptCouponEntity ensureCouponForThreshold(
            ReceiptRedemptionEntity redemption,
            ReceiptMemberEntity member,
            int awardedPoints
    ) {
        if (awardedPoints <= 0) {
            return null;
        }

        if (member.getPointBalance() <= 0 || member.getPointBalance() % COUPON_POINT_THRESHOLD != 0) {
            return null;
        }

        return receiptCouponRepository.findBySourceRedemption_Id(redemption.getId())
                .orElseGet(() -> receiptCouponRepository.save(new ReceiptCouponEntity(
                        member,
                        redemption,
                        generateUniqueCouponCode(),
                        "會員集點 50 元抵用券",
                        COUPON_DISCOUNT_AMOUNT,
                        COUPON_ACTIVE_STATUS,
                        OffsetDateTime.now()
                )));
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

    private String generateUniqueCouponCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder("NC");
            while (builder.length() < COUPON_CODE_LENGTH + 2) {
                builder.append(COUPON_CODE_ALPHABET[secureRandom.nextInt(COUPON_CODE_ALPHABET.length)]);
            }
            code = builder.toString();
        } while (receiptCouponRepository.existsByCouponCode(code));
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

    private record DrawResult(String outcome, ReceiptPrizeEntity prize) {

        static DrawResult win(ReceiptPrizeEntity prize) {
            return new DrawResult(DRAW_OUTCOME_WIN, prize);
        }

        static DrawResult lose() {
            return new DrawResult(DRAW_OUTCOME_LOSE, null);
        }

        boolean won() {
            return prize != null;
        }
    }
}
