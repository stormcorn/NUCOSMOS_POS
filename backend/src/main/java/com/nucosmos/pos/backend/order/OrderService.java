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
import com.nucosmos.pos.backend.order.persistence.OrderItemCustomizationEntity;
import com.nucosmos.pos.backend.order.persistence.PaymentEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptRedemptionEntity;
import com.nucosmos.pos.backend.order.persistence.RefundEntity;
import com.nucosmos.pos.backend.order.repository.OrderItemCustomizationRepository;
import com.nucosmos.pos.backend.order.repository.OrderRepository;
import com.nucosmos.pos.backend.order.repository.PaymentRepository;
import com.nucosmos.pos.backend.order.repository.ReceiptRedemptionRepository;
import com.nucosmos.pos.backend.order.repository.RefundItemRepository;
import com.nucosmos.pos.backend.order.repository.RefundRepository;
import com.nucosmos.pos.backend.product.ProductCustomizationSelectionMode;
import com.nucosmos.pos.backend.product.persistence.ProductCustomizationGroupEntity;
import com.nucosmos.pos.backend.product.persistence.ProductCustomizationOptionEntity;
import com.nucosmos.pos.backend.product.persistence.ProductEntity;
import com.nucosmos.pos.backend.product.repository.ProductCustomizationGroupRepository;
import com.nucosmos.pos.backend.product.repository.ProductCustomizationOptionRepository;
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
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductCustomizationGroupRepository productCustomizationGroupRepository;
    private final ProductCustomizationOptionRepository productCustomizationOptionRepository;
    private final OrderItemCustomizationRepository orderItemCustomizationRepository;
    private final PaymentRepository paymentRepository;
    private final ReceiptRedemptionRepository receiptRedemptionRepository;
    private final RefundRepository refundRepository;
    private final RefundItemRepository refundItemRepository;
    private final CardTerminalService cardTerminalService;
    private final OrderInventoryWorkflowService orderInventoryWorkflowService;
    private final ReceiptRedemptionService receiptRedemptionService;

    public OrderService(
            OrderRepository orderRepository,
            StoreRepository storeRepository,
            DeviceRepository deviceRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductCustomizationGroupRepository productCustomizationGroupRepository,
            ProductCustomizationOptionRepository productCustomizationOptionRepository,
            OrderItemCustomizationRepository orderItemCustomizationRepository,
            PaymentRepository paymentRepository,
            ReceiptRedemptionRepository receiptRedemptionRepository,
            RefundRepository refundRepository,
            RefundItemRepository refundItemRepository,
            CardTerminalService cardTerminalService,
            OrderInventoryWorkflowService orderInventoryWorkflowService,
            ReceiptRedemptionService receiptRedemptionService
    ) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productCustomizationGroupRepository = productCustomizationGroupRepository;
        this.productCustomizationOptionRepository = productCustomizationOptionRepository;
        this.orderItemCustomizationRepository = orderItemCustomizationRepository;
        this.paymentRepository = paymentRepository;
        this.receiptRedemptionRepository = receiptRedemptionRepository;
        this.refundRepository = refundRepository;
        this.refundItemRepository = refundItemRepository;
        this.cardTerminalService = cardTerminalService;
        this.orderInventoryWorkflowService = orderInventoryWorkflowService;
        this.receiptRedemptionService = receiptRedemptionService;
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
        OrderTotals totals = calculateTotals(request.items(), products, orderedAt, request);

        OrderEntity order = new OrderEntity(
                store,
                device,
                createdByUser,
                generateOrderNumber(store.getCode(), orderedAt),
                "CREATED",
                "UNPAID",
                totals.itemCount(),
                totals.subtotalAmount(),
                totals.discountType().name(),
                totals.discountValue(),
                totals.discountAmount(),
                totals.totalAmount(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                blankToNull(request.note()),
                blankToNull(request.discountNote()),
                orderedAt
        );

        List<OrderItemCustomizationEntity> pendingCustomizations = new ArrayList<>();
        int lineNumber = 1;
        for (OrderCreateItemRequest itemRequest : request.items()) {
            ProductEntity product = products.get(itemRequest.productId());
            List<ResolvedCustomizationSelection> selectedOptions = resolveCustomizationSelections(product, itemRequest.selectedOptionIds());
            BigDecimal unitPrice = calculateUnitPrice(product, orderedAt, selectedOptions);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));
            OrderItemEntity item = new OrderItemEntity(
                    order,
                    product,
                    lineNumber++,
                    product.getSku(),
                    product.getName(),
                    unitPrice,
                    itemRequest.quantity(),
                    lineTotal,
                    blankToNull(itemRequest.note())
            );
            order.addItem(item);
            pendingCustomizations.addAll(buildOrderItemCustomizations(item, selectedOptions));
        }

        OrderEntity savedOrder = orderRepository.save(order);
        if (!pendingCustomizations.isEmpty()) {
            orderItemCustomizationRepository.saveAll(pendingCustomizations);
        }
        receiptRedemptionService.ensureForOrder(savedOrder);

        return toResponse(savedOrder);
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
        PaymentAmounts paymentAmounts = normalizePayment(request, remainingAmount, paymentMethod);
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

        boolean fullyPaid = paymentMethod == PaymentMethod.OTHER
                || totalCaptured.compareTo(order.getTotalAmount()) >= 0;
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

    @Transactional
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

    private OrderTotals calculateTotals(
            List<OrderCreateItemRequest> items,
            Map<UUID, ProductEntity> products,
            OffsetDateTime orderedAt,
            OrderCreateRequest request
    ) {
        int itemCount = 0;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderCreateItemRequest item : items) {
            ProductEntity product = products.get(item.productId());
            List<ResolvedCustomizationSelection> selectedOptions = resolveCustomizationSelections(product, item.selectedOptionIds());
            BigDecimal displayPrice = calculateUnitPrice(product, orderedAt, selectedOptions);
            itemCount += item.quantity();
            subtotal = subtotal.add(displayPrice.multiply(BigDecimal.valueOf(item.quantity())));
        }

        AppliedDiscount appliedDiscount = resolveDiscount(request, subtotal);
        BigDecimal totalAmount = subtotal.subtract(appliedDiscount.discountAmount()).setScale(2, RoundingMode.HALF_UP);

        return new OrderTotals(
                itemCount,
                subtotal,
                appliedDiscount.type(),
                appliedDiscount.discountValue(),
                appliedDiscount.discountAmount(),
                totalAmount
        );
    }

    private String generateOrderNumber(String storeCode, OffsetDateTime orderedAt) {
        return storeCode + "-" + orderedAt.withOffsetSameInstant(ZoneOffset.UTC).format(ORDER_NUMBER_FORMAT)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderResponse toResponse(OrderEntity order) {
        Map<UUID, List<OrderItemCustomizationResponse>> customizationsByOrderItemId = orderItemCustomizationRepository
                .findAllByOrderItem_IdInOrderByLineNumberAscCreatedAtAsc(
                        order.getItems().stream().map(OrderItemEntity::getId).toList()
                )
                .stream()
                .collect(Collectors.groupingBy(
                        customization -> customization.getOrderItem().getId(),
                        Collectors.mapping(
                                customization -> new OrderItemCustomizationResponse(
                                        customization.getId(),
                                        customization.getGroupName(),
                                        customization.getOptionName(),
                                        customization.getPriceDelta(),
                                        customization.getLineNumber()
                                ),
                                Collectors.toList()
                        )
                ));

        ReceiptRedemptionEntity receiptRedemption = resolveReceiptRedemption(order);

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
                order.getDiscountType(),
                order.getDiscountValue(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getPaidAmount(),
                order.getChangeAmount(),
                order.getRefundedAmount(),
                order.getCogsAmount(),
                order.getRefundedCogsAmount(),
                order.getNetCogsAmount(),
                order.getGrossProfitAmount(),
                order.getNote(),
                order.getDiscountNote(),
                order.getOrderedAt(),
                order.getClosedAt(),
                order.getVoidedAt(),
                order.getVoidNote(),
                receiptRedemption.getClaimCode(),
                receiptRedemptionService.buildRedeemUrl(receiptRedemption),
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
                                item.getNote(),
                                customizationsByOrderItemId.getOrDefault(item.getId(), List.of())
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

    private ReceiptRedemptionEntity resolveReceiptRedemption(OrderEntity order) {
        ReceiptRedemptionEntity existing = order.getReceiptRedemption();
        if (existing != null) {
            return existing;
        }

        return receiptRedemptionRepository.findByOrder_Id(order.getId())
                .map(found -> {
                    order.setReceiptRedemption(found);
                    return found;
                })
                .orElseGet(() -> receiptRedemptionService.ensureForOrder(order));
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

    private PaymentAmounts normalizePayment(PaymentRequest request, BigDecimal remainingAmount, PaymentMethod paymentMethod) {
        BigDecimal amountApplied = request.amount();
        BigDecimal amountReceived = request.amountReceived() != null ? request.amountReceived() : request.amount();

        if (paymentMethod == PaymentMethod.OTHER) {
            if (amountApplied.compareTo(BigDecimal.ZERO) != 0 || amountReceived.compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException("OTHER payments must use zero amount");
            }
            return new PaymentAmounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        if (amountApplied.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

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

    private AppliedDiscount resolveDiscount(OrderCreateRequest request, BigDecimal subtotal) {
        OrderDiscountType discountType = OrderDiscountType.from(request.discountType());
        BigDecimal discountValue = request.discountValue();

        if (discountType == OrderDiscountType.NONE
                && request.discountAmount() != null
                && request.discountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountType = OrderDiscountType.AMOUNT;
            discountValue = request.discountAmount();
        }

        return switch (discountType) {
            case NONE -> new AppliedDiscount(OrderDiscountType.NONE, null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            case PERCENTAGE -> {
                BigDecimal normalizedValue = normalizePercentageDiscountValue(discountValue);
                BigDecimal discountAmount = subtotal
                        .multiply(normalizedValue)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                yield new AppliedDiscount(discountType, normalizedValue, discountAmount);
            }
            case AMOUNT -> {
                BigDecimal normalizedValue = normalizeAmountDiscountValue(discountValue, subtotal);
                yield new AppliedDiscount(discountType, normalizedValue, normalizedValue);
            }
            case COMPLIMENTARY -> new AppliedDiscount(
                    OrderDiscountType.COMPLIMENTARY,
                    null,
                    subtotal.setScale(2, RoundingMode.HALF_UP)
            );
        };
    }

    private BigDecimal normalizePercentageDiscountValue(BigDecimal discountValue) {
        if (discountValue == null) {
            throw new BadRequestException("Discount percentage is required");
        }

        BigDecimal normalizedValue = discountValue.setScale(2, RoundingMode.HALF_UP);
        if (normalizedValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Discount percentage must be greater than zero");
        }
        if (normalizedValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Discount percentage cannot exceed 100");
        }
        return normalizedValue;
    }

    private BigDecimal normalizeAmountDiscountValue(BigDecimal discountValue, BigDecimal subtotal) {
        if (discountValue == null) {
            throw new BadRequestException("Discount amount is required");
        }

        BigDecimal normalizedValue = discountValue.setScale(2, RoundingMode.HALF_UP);
        if (normalizedValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Discount amount cannot be negative");
        }
        if (normalizedValue.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Discount amount must be greater than zero");
        }
        if (normalizedValue.compareTo(subtotal) > 0) {
            throw new BadRequestException("Discount amount cannot exceed subtotal");
        }
        return normalizedValue;
    }

    private List<ResolvedCustomizationSelection> resolveCustomizationSelections(ProductEntity product, List<UUID> selectedOptionIds) {
        List<ProductCustomizationGroupEntity> groups = productCustomizationGroupRepository
                .findAllByProduct_IdAndActiveTrueOrderByDisplayOrderAscCreatedAtAsc(product.getId());
        if (groups.isEmpty()) {
            return List.of();
        }

        Set<UUID> requestedIds = selectedOptionIds == null ? Set.of() : Set.copyOf(selectedOptionIds);
        Map<UUID, ProductCustomizationOptionEntity> optionMap = productCustomizationOptionRepository
                .findAllByCustomizationGroup_IdInOrderByDisplayOrderAscCreatedAtAsc(groups.stream().map(ProductCustomizationGroupEntity::getId).toList())
                .stream()
                .filter(ProductCustomizationOptionEntity::isActive)
                .collect(Collectors.toMap(ProductCustomizationOptionEntity::getId, option -> option));

        for (UUID selectedOptionId : requestedIds) {
            ProductCustomizationOptionEntity option = optionMap.get(selectedOptionId);
            if (option == null || !option.getCustomizationGroup().getProduct().getId().equals(product.getId())) {
                throw new BadRequestException("One or more customization options are invalid for this product");
            }
        }

        List<ResolvedCustomizationSelection> resolvedSelections = new ArrayList<>();
        for (ProductCustomizationGroupEntity group : groups) {
            List<ProductCustomizationOptionEntity> groupOptions = optionMap.values().stream()
                    .filter(option -> option.getCustomizationGroup().getId().equals(group.getId()))
                    .sorted((left, right) -> Integer.compare(left.getDisplayOrder(), right.getDisplayOrder()))
                    .toList();

            List<ProductCustomizationOptionEntity> selectedGroupOptions = groupOptions.stream()
                    .filter(option -> requestedIds.contains(option.getId()))
                    .toList();

            if (selectedGroupOptions.isEmpty()) {
                selectedGroupOptions = groupOptions.stream()
                        .filter(ProductCustomizationOptionEntity::isDefaultSelected)
                        .toList();
            }

            validateGroupSelections(group, selectedGroupOptions);

            for (ProductCustomizationOptionEntity option : selectedGroupOptions) {
                resolvedSelections.add(new ResolvedCustomizationSelection(group, option));
            }
        }

        return resolvedSelections;
    }

    private void validateGroupSelections(
            ProductCustomizationGroupEntity group,
            List<ProductCustomizationOptionEntity> selectedGroupOptions
    ) {
        int selectionCount = selectedGroupOptions.size();
        if (group.isRequired() && selectionCount == 0) {
            throw new BadRequestException("Required customization option is missing");
        }
        if (selectionCount < group.getMinSelections()) {
            throw new BadRequestException("Customization selection does not meet minimum requirement");
        }
        if (selectionCount > group.getMaxSelections()) {
            throw new BadRequestException("Customization selection exceeds maximum allowed");
        }
        if (group.getSelectionMode() == ProductCustomizationSelectionMode.SINGLE && selectionCount > 1) {
            throw new BadRequestException("Customization group only allows one option");
        }
    }

    private BigDecimal calculateUnitPrice(
            ProductEntity product,
            OffsetDateTime orderedAt,
            List<ResolvedCustomizationSelection> selectedOptions
    ) {
        BigDecimal unitPrice = product.getDisplayPrice(orderedAt);
        for (ResolvedCustomizationSelection selection : selectedOptions) {
            unitPrice = unitPrice.add(selection.option().getPriceDelta());
        }
        return unitPrice;
    }

    private List<OrderItemCustomizationEntity> buildOrderItemCustomizations(
            OrderItemEntity orderItem,
            List<ResolvedCustomizationSelection> selectedOptions
    ) {
        List<OrderItemCustomizationEntity> customizations = new ArrayList<>();
        int customizationLine = 1;
        for (ResolvedCustomizationSelection selection : selectedOptions) {
            customizations.add(OrderItemCustomizationEntity.create(
                    orderItem,
                    selection.group(),
                    selection.option(),
                    customizationLine++
            ));
        }
        return customizations;
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

        if (originalPaymentMethod == PaymentMethod.OTHER) {
            throw new BadRequestException("Non-revenue OTHER payments cannot be refunded");
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
            OrderDiscountType discountType,
            BigDecimal discountValue,
            BigDecimal discountAmount,
            BigDecimal totalAmount
    ) {
    }

    private record AppliedDiscount(
            OrderDiscountType type,
            BigDecimal discountValue,
            BigDecimal discountAmount
    ) {
    }

    private record PaymentAmounts(
            BigDecimal amountApplied,
            BigDecimal amountReceived,
            BigDecimal changeAmount
    ) {
    }

    private record ResolvedCustomizationSelection(
            ProductCustomizationGroupEntity group,
            ProductCustomizationOptionEntity option
    ) {
    }
}
