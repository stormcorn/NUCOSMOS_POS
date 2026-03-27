package com.nucosmos.pos.backend.shift;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.device.persistence.DeviceEntity;
import com.nucosmos.pos.backend.device.repository.DeviceRepository;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;
import com.nucosmos.pos.backend.order.persistence.PaymentEntity;
import com.nucosmos.pos.backend.order.persistence.RefundEntity;
import com.nucosmos.pos.backend.order.repository.OrderRepository;
import com.nucosmos.pos.backend.shift.persistence.ShiftEntity;
import com.nucosmos.pos.backend.shift.repository.ShiftRepository;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final StoreRepository storeRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ShiftService(
            ShiftRepository shiftRepository,
            StoreRepository storeRepository,
            DeviceRepository deviceRepository,
            UserRepository userRepository,
            OrderRepository orderRepository
    ) {
        this.shiftRepository = shiftRepository;
        this.storeRepository = storeRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public ShiftResponse currentShift(AuthenticatedUser user) {
        DeviceEntity device = resolveDevice(user);
        ShiftEntity shift = shiftRepository.findFirstByStore_CodeAndDevice_IdAndStatusOrderByOpenedAtDesc(
                        user.storeCode(),
                        device.getId(),
                        "OPEN"
                )
                .orElseThrow(() -> new NotFoundException("Open shift not found"));
        return toResponse(shift);
    }

    @Transactional
    public ShiftResponse openShift(AuthenticatedUser user, ShiftOpenRequest request) {
        StoreEntity store = resolveStore(user);
        DeviceEntity device = resolveDevice(user);
        UserEntity openedByUser = resolveUser(user);

        shiftRepository.findFirstByStore_CodeAndDevice_IdAndStatusOrderByOpenedAtDesc(user.storeCode(), device.getId(), "OPEN")
                .ifPresent(existing -> {
                    throw new BadRequestException("An open shift already exists for this device");
                });

        ShiftEntity shift = new ShiftEntity(
                store,
                device,
                openedByUser,
                request.openingCashAmount(),
                blankToNull(request.note()),
                OffsetDateTime.now()
        );

        return toResponse(shiftRepository.save(shift));
    }

    @Transactional
    public ShiftResponse closeShift(UUID shiftId, AuthenticatedUser user, ShiftCloseRequest request) {
        ShiftEntity shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("Shift not found"));

        if (!shift.getStore().getCode().equals(user.storeCode())) {
            throw new NotFoundException("Shift not found");
        }
        if (!"OPEN".equals(shift.getStatus())) {
            throw new BadRequestException("Shift is already closed");
        }

        UserEntity closedByUser = resolveUser(user);
        OffsetDateTime closedAt = OffsetDateTime.now();

        ShiftTotals totals = calculateShiftTotals(
                shift.getStore().getCode(),
                shift.getDevice().getId(),
                shift.getOpenedAt(),
                closedAt
        );

        BigDecimal expectedCashAmount = shift.getOpeningCashAmount()
                .add(totals.cashSalesAmount())
                .subtract(totals.cashRefundedAmount());

        shift.close(
                closedByUser,
                request.closingCashAmount(),
                expectedCashAmount,
                totals.cashSalesAmount(),
                totals.cardSalesAmount(),
                totals.refundedAmount(),
                totals.netSalesAmount(),
                totals.orderCount(),
                totals.voidedOrderCount(),
                blankToNull(request.note()),
                closedAt
        );

        return toResponse(shift);
    }

    private ShiftTotals calculateShiftTotals(String storeCode, UUID deviceId, OffsetDateTime openedAt, OffsetDateTime closedAt) {
        List<OrderEntity> orders = orderRepository.findAllByStore_CodeAndDevice_IdAndOrderedAtBetweenOrderByOrderedAtAsc(
                storeCode,
                deviceId,
                openedAt,
                closedAt
        );

        BigDecimal grossSales = BigDecimal.ZERO;
        BigDecimal refundedAmount = BigDecimal.ZERO;
        BigDecimal cashSalesAmount = BigDecimal.ZERO;
        BigDecimal cardSalesAmount = BigDecimal.ZERO;
        BigDecimal cashRefundedAmount = BigDecimal.ZERO;
        int orderCount = 0;
        int voidedOrderCount = 0;

        for (OrderEntity order : orders) {
            if ("VOIDED".equals(order.getStatus())) {
                voidedOrderCount++;
            } else {
                orderCount++;
                grossSales = grossSales.add(recognizedGrossAmount(order));
                refundedAmount = refundedAmount.add(recognizedRefundedAmount(order));
            }

            for (PaymentEntity payment : order.getPayments()) {
                if (!isSettledPayment(payment)) {
                    continue;
                }
                if ("CASH".equals(payment.getPaymentMethod())) {
                    cashSalesAmount = cashSalesAmount.add(payment.getAmount());
                } else if ("CARD".equals(payment.getPaymentMethod())) {
                    cardSalesAmount = cardSalesAmount.add(payment.getAmount());
                }
            }

            for (RefundEntity refund : order.getRefunds()) {
                if ("CASH".equals(refund.getRefundMethod())) {
                    cashRefundedAmount = cashRefundedAmount.add(refund.getAmount());
                }
            }
        }

        return new ShiftTotals(
                cashSalesAmount,
                cardSalesAmount,
                refundedAmount,
                cashRefundedAmount,
                grossSales.subtract(refundedAmount),
                orderCount,
                voidedOrderCount
        );
    }

    private boolean isSettledPayment(PaymentEntity payment) {
        return "CAPTURED".equals(payment.getStatus()) || "REFUNDED".equals(payment.getStatus());
    }

    private BigDecimal recognizedGrossAmount(OrderEntity order) {
        return isNonRevenueOtherOrder(order) ? BigDecimal.ZERO : order.getTotalAmount();
    }

    private BigDecimal recognizedRefundedAmount(OrderEntity order) {
        return isNonRevenueOtherOrder(order) ? BigDecimal.ZERO : order.getRefundedAmount();
    }

    private boolean isNonRevenueOtherOrder(OrderEntity order) {
        List<PaymentEntity> settledPayments = order.getPayments().stream()
                .filter(this::isSettledPayment)
                .toList();
        return !settledPayments.isEmpty()
                && settledPayments.stream().allMatch(payment -> "OTHER".equals(payment.getPaymentMethod()));
    }

    private StoreEntity resolveStore(AuthenticatedUser user) {
        return storeRepository.findByCodeAndStatus(user.storeCode(), "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated store is not available"));
    }

    private DeviceEntity resolveDevice(AuthenticatedUser user) {
        if (user.deviceCode() == null || user.deviceCode().isBlank()) {
            throw new BadRequestException("Authenticated device is not available");
        }
        return deviceRepository.findByStore_CodeAndDeviceCodeAndStatus(user.storeCode(), user.deviceCode(), "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated device is not available"));
    }

    private UserEntity resolveUser(AuthenticatedUser user) {
        return userRepository.findById(user.userId())
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));
    }

    private ShiftResponse toResponse(ShiftEntity shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getStatus(),
                shift.getStore().getCode(),
                shift.getDevice().getDeviceCode(),
                shift.getOpenedByUser().getEmployeeCode(),
                shift.getClosedByUser() == null ? null : shift.getClosedByUser().getEmployeeCode(),
                shift.getOpeningCashAmount(),
                shift.getClosingCashAmount(),
                shift.getExpectedCashAmount(),
                shift.getCashSalesAmount(),
                shift.getCardSalesAmount(),
                shift.getRefundedAmount(),
                shift.getNetSalesAmount(),
                shift.getOrderCount(),
                shift.getVoidedOrderCount(),
                shift.getNote(),
                shift.getCloseNote(),
                shift.getOpenedAt(),
                shift.getClosedAt()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ShiftTotals(
            BigDecimal cashSalesAmount,
            BigDecimal cardSalesAmount,
            BigDecimal refundedAmount,
            BigDecimal cashRefundedAmount,
            BigDecimal netSalesAmount,
            int orderCount,
            int voidedOrderCount
    ) {
    }
}
