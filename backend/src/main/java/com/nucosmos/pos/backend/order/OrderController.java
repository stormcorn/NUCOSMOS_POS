package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.api.ApiResponse;
import com.nucosmos.pos.backend.common.api.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(orderService.createOrder(user, request));
    }

    @GetMapping
    public ApiResponse<PagedResponse<OrderSummaryResponse>> listOrders(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "orderedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(orderService.listOrders(user, status, paymentStatus, from, to, page, size, sortBy, sortDirection));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            Authentication authentication
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(orderService.getOrder(orderId, user));
    }

    @PostMapping("/{orderId}/payments")
    public ApiResponse<OrderResponse> addPayment(
            @PathVariable UUID orderId,
            Authentication authentication,
            @Valid @RequestBody PaymentRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(orderService.addPayment(orderId, user, request));
    }

    @PostMapping("/{orderId}/payments/authorize")
    public ApiResponse<OrderResponse> authorizeCardPayment(
            @PathVariable UUID orderId,
            Authentication authentication,
            @Valid @RequestBody CardAuthorizationRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(orderService.authorizeCardPayment(orderId, user, request));
    }

    @PostMapping("/{orderId}/payments/{paymentId}/capture")
    public ApiResponse<OrderResponse> captureAuthorizedCardPayment(
            @PathVariable UUID orderId,
            @PathVariable UUID paymentId,
            Authentication authentication,
            @Valid @RequestBody CardCaptureRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(orderService.captureAuthorizedCardPayment(orderId, paymentId, user, request));
    }

    @PostMapping("/{orderId}/refunds")
    public ApiResponse<OrderResponse> refundOrder(
            @PathVariable UUID orderId,
            Authentication authentication,
            @Valid @RequestBody RefundRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(orderService.refundOrder(orderId, user, request));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable UUID orderId,
            Authentication authentication,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(orderService.cancelOrder(orderId, user, request));
    }
}
