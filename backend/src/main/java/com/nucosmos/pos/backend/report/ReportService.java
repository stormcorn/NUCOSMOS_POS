package com.nucosmos.pos.backend.report;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;
import com.nucosmos.pos.backend.order.persistence.PaymentEntity;
import com.nucosmos.pos.backend.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse salesSummary(AuthenticatedUser user, OffsetDateTime from, OffsetDateTime to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be before or equal to to");
        }

        List<OrderEntity> orders = orderRepository.findAllByStore_CodeAndOrderedAtBetweenOrderByOrderedAtAsc(
                user.storeCode(),
                from,
                to
        );

        BigDecimal grossSalesAmount = BigDecimal.ZERO;
        BigDecimal refundedAmount = BigDecimal.ZERO;
        BigDecimal cashSalesAmount = BigDecimal.ZERO;
        BigDecimal cardSalesAmount = BigDecimal.ZERO;
        int orderCount = 0;
        int voidedOrderCount = 0;

        for (OrderEntity order : orders) {
            if ("VOIDED".equals(order.getStatus())) {
                voidedOrderCount++;
            } else {
                orderCount++;
                grossSalesAmount = grossSalesAmount.add(order.getTotalAmount());
                refundedAmount = refundedAmount.add(order.getRefundedAmount());
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
        }

        BigDecimal netSalesAmount = grossSalesAmount.subtract(refundedAmount);
        BigDecimal averageOrderAmount = orderCount == 0
                ? BigDecimal.ZERO
                : netSalesAmount.divide(BigDecimal.valueOf(orderCount), 2, java.math.RoundingMode.HALF_UP);

        return new SalesSummaryResponse(
                user.storeCode(),
                from,
                to,
                orderCount,
                voidedOrderCount,
                grossSalesAmount,
                refundedAmount,
                netSalesAmount,
                cashSalesAmount,
                cardSalesAmount,
                averageOrderAmount
        );
    }

    private boolean isSettledPayment(PaymentEntity payment) {
        return "CAPTURED".equals(payment.getStatus()) || "REFUNDED".equals(payment.getStatus());
    }
}
