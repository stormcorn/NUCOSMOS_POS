package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import com.nucosmos.pos.backend.order.persistence.ReceiptMemberEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/public/redeem")
public class PublicRedeemController {

    private final ReceiptRedemptionService receiptRedemptionService;
    private final PublicMemberAuthService publicMemberAuthService;

    public PublicRedeemController(
            ReceiptRedemptionService receiptRedemptionService,
            PublicMemberAuthService publicMemberAuthService
    ) {
        this.receiptRedemptionService = receiptRedemptionService;
        this.publicMemberAuthService = publicMemberAuthService;
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
    public ApiResponse<ReceiptRedeemResponse> claim(
            @PathVariable String token,
            @Valid @RequestBody(required = false) PublicRedeemClaimRequest request,
            @CookieValue(value = PublicMemberAuthService.SESSION_COOKIE_NAME, required = false) String sessionToken,
            @RequestHeader(value = "X-Nucosmos-Device-Token", required = false) String deviceToken,
            @RequestHeader(value = "X-Nucosmos-Device-Label", required = false) String deviceLabel,
            HttpServletResponse response
    ) {
        PublicMemberAuthService.AuthenticatedMemberResult authenticated = publicMemberAuthService.resolveAuthenticatedMember(
                sessionToken,
                deviceToken,
                deviceLabel
        ).orElse(null);
        if (authenticated != null && authenticated.sessionCookie() != null) {
            response.addHeader("Set-Cookie", authenticated.sessionCookie().toString());
        }
        ReceiptMemberEntity authenticatedMember = authenticated == null ? null : authenticated.member();
        return ApiResponse.ok(receiptRedemptionService.claimByToken(token, request, authenticatedMember));
    }
}
