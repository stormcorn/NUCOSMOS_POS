package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/member")
public class PublicMemberAuthController {

    private final PublicMemberAuthService publicMemberAuthService;

    public PublicMemberAuthController(PublicMemberAuthService publicMemberAuthService) {
        this.publicMemberAuthService = publicMemberAuthService;
    }

    @GetMapping("/firebase-config")
    public ApiResponse<PublicMemberFirebaseConfigResponse> firebaseConfig() {
        return ApiResponse.ok(publicMemberAuthService.firebaseConfig());
    }

    @PostMapping("/login/sms")
    public ApiResponse<PublicMemberSessionResponse> loginWithSms(
            @Valid @RequestBody PublicMemberLoginRequest request,
            @RequestHeader(value = "X-Nucosmos-Device-Token", required = false) String deviceToken,
            @RequestHeader(value = "X-Nucosmos-Device-Label", required = false) String deviceLabel,
            HttpServletResponse response
    ) {
        PublicMemberAuthService.LoginResult result = publicMemberAuthService.loginWithSms(request, deviceToken, deviceLabel);
        response.addHeader("Set-Cookie", result.sessionCookie().toString());
        return ApiResponse.ok(result.session());
    }

    @GetMapping("/session")
    public ApiResponse<PublicMemberSessionResponse> currentSession(
            @CookieValue(value = PublicMemberAuthService.SESSION_COOKIE_NAME, required = false) String sessionToken,
            @RequestHeader(value = "X-Nucosmos-Device-Token", required = false) String deviceToken,
            @RequestHeader(value = "X-Nucosmos-Device-Label", required = false) String deviceLabel,
            HttpServletResponse response
    ) {
        PublicMemberAuthService.SessionResult result = publicMemberAuthService.currentSession(sessionToken, deviceToken, deviceLabel);
        if (result.sessionCookie() != null) {
            response.addHeader("Set-Cookie", result.sessionCookie().toString());
        }
        return ApiResponse.ok(result.session());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = PublicMemberAuthService.SESSION_COOKIE_NAME, required = false) String sessionToken,
            @RequestHeader(value = "X-Nucosmos-Device-Token", required = false) String deviceToken,
            HttpServletResponse response
    ) {
        publicMemberAuthService.logout(sessionToken, deviceToken);
        response.addHeader("Set-Cookie", publicMemberAuthService.clearSessionCookie().toString());
        return ApiResponse.ok(null);
    }
}
