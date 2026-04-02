package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/redeem-prizes")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ReceiptPrizeAdminController {

    private final ReceiptPrizeAdminService receiptPrizeAdminService;

    public ReceiptPrizeAdminController(ReceiptPrizeAdminService receiptPrizeAdminService) {
        this.receiptPrizeAdminService = receiptPrizeAdminService;
    }

    @GetMapping
    public ApiResponse<List<ReceiptPrizeAdminResponse>> listPrizes() {
        return ApiResponse.ok(receiptPrizeAdminService.listPrizes());
    }

    @PostMapping
    public ApiResponse<ReceiptPrizeAdminResponse> createPrize(@Valid @RequestBody ReceiptPrizeAdminRequest request) {
        return ApiResponse.ok(receiptPrizeAdminService.createPrize(request));
    }

    @PutMapping("/{prizeId}")
    public ApiResponse<ReceiptPrizeAdminResponse> updatePrize(
            @PathVariable UUID prizeId,
            @Valid @RequestBody ReceiptPrizeAdminRequest request
    ) {
        return ApiResponse.ok(receiptPrizeAdminService.updatePrize(prizeId, request));
    }

    @PostMapping("/{prizeId}/deactivate")
    public ApiResponse<ReceiptPrizeAdminResponse> deactivatePrize(@PathVariable UUID prizeId) {
        return ApiResponse.ok(receiptPrizeAdminService.deactivatePrize(prizeId));
    }
}
