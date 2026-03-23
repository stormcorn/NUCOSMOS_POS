package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.api.PagedResponse;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.common.exception.NotFoundException;
import com.nucosmos.pos.backend.device.persistence.DeviceEntity;
import com.nucosmos.pos.backend.device.repository.DeviceRepository;
import com.nucosmos.pos.backend.order.persistence.OrderEntity;
import com.nucosmos.pos.backend.order.persistence.OrderItemEntity;
import com.nucosmos.pos.backend.order.persistence.PaymentEntity;
import com.nucosmos.pos.backend.order.persistence.RefundEntity;
import com.nucosmos.pos.backend.order.repository.OrderRepository;
import com.nucosmos.pos.backend.order.repository.PaymentRepository;
import com.nucosmos.pos.backend.order.repository.RefundItemRepository;
import com.nucosmos.pos.backend.order.repository.RefundRepository;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.repository.ProductRepository;
import com.nucosmos.pos.backend.store.persistence.StoreEntity;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final CardTerminalService cardTerminalService;
    private final OrderInventoryWorkflowService orderInventoryWorkflowService;

    public OrderService(
            OrderRepository orderRepository,
            StoreRepository storeRepository,
            DeviceRepository deviceRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            RefundItemRepository refundItemRepository,
            CardTerminalService cardTerminalService,
            OrderInventoryWorkflowService orderInventoryWorkflowService
    ) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.refundItemRepository = refundItemRepository;
        this.cardTerminalService = cardTerminalService;
        this.orderInventoryWorkflowService = orderInventoryWorkflowService;
    }

    @Transactional
    public OrderResponse createOrder(AuthenticatedUser user, OrderCreateRequest request) {
        StoreEntity store = storeRepository.findByCodeAndStatus(user.storeCode(), "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated store is not available"));

        UserEntity createdByUser = userRepository.findById(user.userId())
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));

        DeviceEntity device = resolveDevice(user);
        Map<UUID, ProductEntity> products = loadProducts(request.items());

        OffsetDateTime orderedAt = OffsetDateTime.now();
        OrderTotals totals = calculateTotals(request.items(), products, orderedAt);

        OrderEntity order = new OrderEntity(
                store,
                device,
                createdByUser,
                generateOrderNumber(store.getCode(), orderedAt),
                "CREATED",
                "UNPAID",
                totals.itemCount(),
                totals.subtotalAmount(),
                totals.totalAmount(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                blankToNull(request.note()),
                orderedAt
        );

        int lineNumber = 1;
        for (OrderCreateItemRequest itemRequest : request.items()) {
            ProductEntity product = products.get(itemRequest.productId());
            BigDecimal displayPrice = product.getDisplayPrice(orderedAt);
            BigDecimal lineTotal = displayPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));
            OrderItemEntity item = new OrderItemEntity(
                    order,
                    product,
                    lineNumber++,
                    product.getSku(),
                    product.getName(),
                    displayPrice,
                    itemRequest.quantity(),
                    lineTotal,
                    blankToNull(itemRequest.note())
            );
            order.addItem(item);
        }

        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse addPayment(UUID orderId, AuthenticatedUser user, PaymentRequest request) {
        OrderEntity order = getOrderForStore(orderId, user.storeCode());

        if ("PAID".equals(order.getPaymentStatus())) {
            throw new BadRequestException("Order is already fully paid");
        }

        UserEntity createdByUser = userRepository.findById(user.userId())
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));

        PaymentMethod paymentMethod = PaymentMethod.from(request.paymentMethod());
        if (paymentMethod == PaymentMethod.CARD) {
            throw new BadRequestException("Card payments must be authorized before capture");
        }

        BigDecimal remainingAmount = order.getTotalAmount().subtract(order.getPaidAmount());
        PaymentAmounts paymentAmounts = normalizePayment(request, remainingAmount);
        OffsetDateTime paidAt = OffsetDateTime.now();

        PaymentEntity payment = new PaymentEntity(
                order,
                createdByUser,
                paymentMethod.name(),
                "CAPTURED",
                paymentAmounts.amountApplied(),
                paymentAmounts.amountReceived(),
                paymentAmounts.changeAmount(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                paidAt,
                null,
                null,
                blankToNull(request.note()),
                paidAt
        );

        paymentRepository.save(payment);
        order.addPayment(payment);

        BigDecimal totalCaptured = order.getPayments().stream()
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalChange = order.getPayments().stream()
                .map(PaymentEntity::getChangeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean fullyPaid = totalCaptured.compareTo(order.getTotalAmount()) >= 0;
        order.applyPayment(totalCaptured, totalChange, fullyPaid ? paidAt : null, fullyPaid);
        if (fullyPaid) {
            orderInventoryWorkflowService.commitOrderInventory(order, createdByUser);
        }

        return toResponse(order);
    }

    @Transactional
    public OrderResponse authorizeCardPayment(UUID orderId, AuthenticatedUser user, CardAuthorizationRequest request) {
        OrderEntity order = getOrderForStore(orderId, user.storeCode());

        if ("PAID".equals(order.getPaymentStatus())) {
            throw new BadRequestException("Order is already fully paid");
        }

        UserEntity createdByUser = userRepository.findById(user.userId())
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));

        BigDecimal remainingAmount = order.getTotalAmount().subtract(sumSettledPayments(order));
        if (request.amount().compareTo(remainingAmount) > 0) {
            throw new BadRequestException("Authorized amount exceeds remaining balance");
        }

        OffsetDateTime authorizedAt = OffsetDateTime.now();
        CardTransactionResult cardTransaction = cardTerminalService.authorize(new CardAuthorizationCommand(order, user, request.amount()));

        PaymentEntity payment = new PaymentEntity(
                order,
                createdByUser,
                PaymentMethod.CARD.name(),
                "AUTHORIZED",
                request.amount(),
                null,
                BigDecimal.ZERO,
                cardTransaction.provider().name(),
                cardTransaction.status().name(),
                cardTransaction.terminalTransactionId(),
                cardTransaction.approvalCode(),
                cardTransaction.maskedPan(),
                cardTransaction.batchNumber(),
                cardTransaction.retrievalReferenceNumber(),
                cardTransaction.entryMode(),
                authorizedAt,
                null,
                null,
                null,
                blankToNull(request.note()),
                authorizedAt
        );

        paymentRepository.save(payment);
        order.addPayment(payment);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse captureAuthorizedCardPayment(UUID orderId, UUID paymentId, AuthenticatedUser user, CardCaptureRequest request) {
        OrderEntity order = getOrderForStore(orderId, user.storeCode());

        PaymentEntity payment = order.getPayments().stream()
                .filter(candidate -> candidate.getId().equals(paymentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        if (!PaymentMethod.CARD.name().equals(payment.getPaymentMethod())) {
            throw new BadRequestException("Only card payments can be captured");
        }
        if (!"AUTHORIZED".equals(payment.getStatus())) {
            throw new BadRequestException("Only authorized payments can be captured");
        }

        OffsetDateTime capturedAt = OffsetDateTime.now();
        CardTransactionResult cardTransaction = cardTerminalService.capture(
                new CardCaptureCommand(order, user, payment.getAmount(), payment.getCardTerminalTxnId())
        );

        payment.captureCardPayment(
                cardTransaction.status().name(),
                cardTransaction.terminalTransactionId(),
                cardTransaction.approvalCode(),
                cardTransaction.maskedPan(),
                cardTransaction.batchNumber(),
                cardTransaction.retrievalReferenceNumber(),
                cardTransaction.entryMode(),
                capturedAt,
                blankToNull(request.note())
        );

        BigDecimal totalCaptured = sumSettledPayments(order);
        BigDecimal totalChange = order.getPayments().stream()
                .map(PaymentEntity::getChangeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean fullyPaid = totalCaptured.compareTo(order.getTotalAmount()) >= 0;
        order.applyPayment(totalCaptured, totalChange, fullyPaid ? capturedAt : null, fullyPaid);
        if (fullyPaid) {
            orderInventoryWorkflowService.commitOrderInventory(order, payment.getCreatedByUser());
        }

        return toResponse(order);
    }

    @Transactional
    public OrderResponse refundOrder(UUID orderId, AuthenticatedUser user, RefundRequest request) {
        OrderEntity order = getOrderForStore(orderId, user.storeCode());

        if (!"PAID".equals(order.getPaymentStatus()) && !"PARTIALLY_REFUNDED".equals(order.getPaymentStatus())) {
            throw new BadRequestException("Only paid orders can be refunded");
        }

        UserEntity createdByUser = userRepository.findById(user.userId())
                .orElseThrow(() -> new BadRequestException("Authenticated user is not available"));

        PaymentEntity payment = order.getPayments().stream()
                .filter(candidate -> candidate.getId().equals(request.paymentId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Referenced payment does not belong to this order"));
        if (!"CAPTURED".equals(payment.getStatus()) && !"REFUNDED".equals(payment.getStatus())) {
            throw new BadRequestException("Only captured payments can be refunded");
        }
        RefundMethod refundMethod = RefundMethod.from(request.refundMethod());
        validateRefundMethod(payment, refundMethod);

        BigDecimal remainingRefundable = order.getPaidAmount().subtract(order.getRefundedAmount());
        if (request.amount().compareTo(remainingRefundable) > 0) {
            throw new BadRequestException("Refund amount exceeds refundable balance");
        }

        BigDecimal refundedAgainstPayment = order.getRefunds().stream()
                .filter(refund -> refund.getPayment().getId().equals(payment.getId()))
                .map(RefundEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingPaymentRefundable = payment.getAmount().subtract(refundedAgainstPayment);
        if (request.amount().compareTo(remainingPaymentRefundable) > 0) {
            throw new BadRequestException("Refund amount exceeds refundable balance for the selected payment");
        }

        OffsetDateTime refundedAt = OffsetDateTime.now();
        RefundEntity refund = new RefundEntity(
                order,
                createdByUser,
                payment,
                refundMethod.name(),
                request.amount(),
                blankToNull(request.reason()),
                "PROCESSED",
                refundedAt
        );

        refundRepository.save(refund);
        order.addRefund(refund);
        orderInventoryWorkflowService.restoreRefundInventory(order, refund, request.items(), createdByUser);

        boolean fullyRefundedPayment = request.amount().compareTo(remainingPaymentRefundable) >= 0;
        payment.applyCardRefundLifecycle(refundedAt, fullyRefundedPayment);

        BigDecimal totalRefunded = order.getRefunds().stream()
                .map(RefundEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.applyRefund(totalRefunded, refundedAt);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, AuthenticatedUser user, CancelOrderRequest request) {
        OrderEntity order = getOrderForStore(orderId, user.storeCode());

        if (!"UNPAID".equals(order.getPaymentStatus())) {
            throw new BadRequestException("Only unpaid orders can be voided");
        }
        if ("VOIDED".equals(order.getStatus())) {
            throw new BadRequestException("Order is already voided");
        }

        OffsetDateTime voidedAt = OffsetDateTime.now();
        for (PaymentEntity payment : order.getPayments()) {
            if (PaymentMethod.CARD.name().equals(payment.getPaymentMethod()) && "AUTHORIZED".equals(payment.getStatus())) {
                cardTerminalService.voidTransaction(new CardVoidCommand(order, user, payment.getCardTerminalTxnId(), request.reason()));
                payment.voidCardAuthorization(voidedAt);
            }
        }

        order.voidOrder(voidedAt, blankToNull(request.reason()));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, AuthenticatedUser user) {
        return toResponse(getOrderForStore(orderId, user.storeCode()));
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> listOrders(
            AuthenticatedUser user,
            String status,
            String paymentStatus,
            OffsetDateTime from,
            OffsetDateTime to,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        String normalizedStatus = normalizeFilter(status);
        String normalizedPaymentStatus = normalizeFilter(paymentStatus);
        validateDateRange(from, to);

        Pageable pageable = PageRequest.of(page, size, Sort.by(resolveSortDirection(sortDirection), resolveSortBy(sortBy)));
        Page<OrderEntity> result = orderRepository.findAll(
                buildListSpecification(user.storeCode(), normalizedStatus, normalizedPaymentStatus, from, to),
                pageable
        );

        return new PagedResponse<>(
                result.getContent().stream()
                        .map(this::toSummaryResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    private DeviceEntity resolveDevice(AuthenticatedUser user) {
        if (user.deviceCode() == null || user.deviceCode().isBlank()) {
            return null;
        }
        return deviceRepository.findByStore_CodeAndDeviceCodeAndStatus(user.storeCode(), user.deviceCode(), "ACTIVE")
                .orElseThrow(() -> new BadRequestException("Authenticated device is not available"));
    }

    private Map<UUID, ProductEntity> loadProducts(List<OrderCreateItemRequest> items) {
        List<UUID> productIds = items.stream().map(OrderCreateItemRequest::productId).distinct().toList();
        Map<UUID, ProductEntity> products = new LinkedHashMap<>();
        for (ProductEntity product : productRepository.findAllById(productIds)) {
            products.put(product.getId(), product);
        }

        for (UUID productId : productIds) {
            ProductEntity product = products.get(productId);
            if (product == null || !product.isActive()) {
                throw new BadRequestException("One or more products are invalid or unavailable");
            }
        }
        return products;
    }

    private OrderTotals calculateTotals(List<OrderCreateItemRequest> items, Map<UUID, ProductEntity> products, OffsetDateTime orderedAt) {
        int itemCount = 0;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderCreateItemRequest item : items) {
            ProductEntity product = products.get(item.productId());
            BigDecimal displayPrice = product.getDisplayPrice(orderedAt);
            itemCount += item.quantity();
            subtotal = subtotal.add(displayPrice.multiply(BigDecimal.valueOf(item.quantity())));
        }

        return new OrderTotals(itemCount, subtotal, subtotal);
    }

    private String generateOrderNumber(String storeCode, OffsetDateTime orderedAt) {
        return storeCode + "-" + orderedAt.withOffsetSameInstant(ZoneOffset.UTC).format(ORDER_NUMBER_FORMAT)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderResponse toResponse(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getStore().getCode(),
                order.getDevice() != null ? order.getDevice().getDeviceCode() : null,
                order.getCreatedByUser().getEmployeeCode(),
                order.getItemCount(),
                order.getSubtotalAmount(),
                order.getTotalAmount(),
                order.getPaidAmount(),
                order.getChangeAmount(),
                order.getRefundedAmount(),
                order.getCogsAmount(),
                order.getRefundedCogsAmount(),
                order.getNetCogsAmount(),
                order.getGrossProfitAmount(),
                order.getNote(),
                order.getOrderedAt(),
                order.getClosedAt(),
                order.getVoidedAt(),
                order.getVoidNote(),
                order.getItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getId(),
                                item.getLineNumber(),
                                item.getProduct().getId(),
                                item.getProductSku(),
                                item.getProductName(),
                                item.getUnitPrice(),
                                item.getQuantity(),
                                item.getLineTotalAmount(),
                                item.getUnitCostAmount(),
                                item.getLineCostAmount(),
                                item.getRefundedCostAmount(),
                                item.getNote()
                        ))
                        .toList(),
                order.getPayments().stream()
                        .map(payment -> new PaymentResponse(
                                payment.getId(),
                                payment.getPaymentMethod(),
                                payment.getStatus(),
                                payment.getAmount(),
                                payment.getAmountReceived(),
                                payment.getChangeAmount(),
                                payment.getCardTerminalProvider(),
                                payment.getCardTransactionStatus(),
                                payment.getCardTerminalTxnId(),
                                payment.getCardApprovalCode(),
                                payment.getCardMaskedPan(),
                                payment.getCardBatchNo(),
                                payment.getCardRrn(),
                                payment.getCardEntryMode(),
                                payment.getAuthorizedAt(),
                                payment.getCapturedAt(),
                                payment.getVoidedAt(),
                                payment.getRefundedAt(),
                                payment.getCreatedByUser().getEmployeeCode(),
                                payment.getNote(),
                                payment.getPaidAt()
                        ))
                        .toList(),
                order.getRefunds().stream()
                        .map(refund -> new RefundResponse(
                                refund.getId(),
                                refund.getPayment().getId(),
                                refund.getRefundMethod(),
                                refund.getAmount(),
                                refund.getReason(),
                                refund.getStatus(),
                                refund.getCreatedByUser().getEmployeeCode(),
                                refund.getRefundedAt(),
                                refund.getRefundItems().stream()
                                        .map(item -> new RefundItemResponse(
                                                item.getId(),
                                                item.getOrderItem().getId(),
                                                item.getProduct().getId(),
                                                item.getProduct().getSku(),
                                                item.getProduct().getName(),
                                                item.getQuantity(),
                                                item.getInventoryDisposition()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    private OrderSummaryResponse toSummaryResponse(OrderEntity order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getStore().getCode(),
                order.getDevice() != null ? order.getDevice().getDeviceCode() : null,
                order.getCreatedByUser().getEmployeeCode(),
                order.getItemCount(),
                order.getTotalAmount(),
                order.getPaidAmount(),
                order.getRefundedAmount(),
                order.getCogsAmount(),
                order.getNetCogsAmount(),
                order.getGrossProfitAmount(),
                order.getOrderedAt(),
                order.getClosedAt()
        );
    }

    private OrderEntity getOrderForStore(UUID orderId, String storeCode) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getStore().getCode().equals(storeCode)) {
            throw new NotFoundException("Order not found");
        }
        return order;
    }

    private PaymentAmounts normalizePayment(PaymentRequest request, BigDecimal remainingAmount) {
        BigDecimal amountApplied = request.amount();
        BigDecimal amountReceived = request.amountReceived() != null ? request.amountReceived() : request.amount();

        if (amountApplied.compareTo(remainingAmount) > 0) {
            throw new BadRequestException("Payment amount exceeds remaining balance");
        }

        if (amountReceived.compareTo(amountApplied) < 0) {
            throw new BadRequestException("Received amount cannot be less than applied amount");
        }

        BigDecimal changeAmount = amountReceived.subtract(amountApplied);
        return new PaymentAmounts(amountApplied, amountReceived, changeAmount);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private BigDecimal sumSettledPayments(OrderEntity order) {
        return order.getPayments().stream()
                .filter(payment -> "CAPTURED".equals(payment.getStatus()) || "REFUNDED".equals(payment.getStatus()))
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateRefundMethod(PaymentEntity payment, RefundMethod refundMethod) {
        PaymentMethod originalPaymentMethod = PaymentMethod.from(payment.getPaymentMethod());

        if (originalPaymentMethod == PaymentMethod.CASH && refundMethod != RefundMethod.CASH) {
            throw new BadRequestException("Cash payments must use CASH refund method");
        }

        if (originalPaymentMethod == PaymentMethod.CARD
                && refundMethod != RefundMethod.CARD_REFUND
                && refundMethod != RefundMethod.CARD_REVERSAL) {
            throw new BadRequestException("Card payments must use CARD_REFUND or CARD_REVERSAL");
        }
    }

    private Specification<OrderEntity> buildListSpecification(
            String storeCode,
            String status,
            String paymentStatus,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("store").get("code"), storeCode));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (paymentStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("orderedAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("orderedAt"), to));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must be earlier than or equal to 'to'");
        }
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "orderedAt";
        }

        String normalized = sortBy.trim();
        return switch (normalized) {
            case "orderedAt", "closedAt", "totalAmount", "paidAmount" -> normalized;
            default -> throw new BadRequestException("Unsupported sortBy field");
        };
    }

    private Sort.Direction resolveSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return Sort.Direction.DESC;
        }

        return switch (sortDirection.trim().toLowerCase()) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BadRequestException("Unsupported sortDirection value");
        };
    }

    private record OrderTotals(
            int itemCount,
            BigDecimal subtotalAmount,
            BigDecimal totalAmount
    ) {
    }

    private record PaymentAmounts(
            BigDecimal amountApplied,
            BigDecimal amountReceived,
            BigDecimal changeAmount
    ) {
    }
}
