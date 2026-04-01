package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/public/redeem")
public class PublicRedeemController {

    private final ReceiptRedemptionService receiptRedemptionService;

    public PublicRedeemController(ReceiptRedemptionService receiptRedemptionService) {
        this.receiptRedemptionService = receiptRedemptionService;
    }

    @GetMapping("/{token}")
    public ApiResponse<ReceiptRedeemResponse> lookupByToken(@PathVariable String token) {
        return ApiResponse.ok(receiptRedemptionService.getByToken(token));
    }

    @GetMapping("/search")
    public ApiResponse<ReceiptRedeemResponse> lookupByClaimCode(
            @RequestParam @NotBlank String code
    ) {
        return ApiResponse.ok(receiptRedemptionService.getByClaimCode(code));
    }

    @PostMapping("/{token}/claim")
    public ApiResponse<ReceiptRedeemResponse> claim(@PathVariable String token) {
        return ApiResponse.ok(receiptRedemptionService.claimByToken(token));
    }
}
