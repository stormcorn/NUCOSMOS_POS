package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
            HttpServletResponse response
    ) {
        PublicMemberAuthService.LoginResult result = publicMemberAuthService.loginWithSms(request);
        response.addHeader("Set-Cookie", result.sessionCookie().toString());
        return ApiResponse.ok(result.session());
    }

    @GetMapping("/session")
    public ApiResponse<PublicMemberSessionResponse> currentSession(
            @CookieValue(value = PublicMemberAuthService.SESSION_COOKIE_NAME, required = false) String sessionToken
    ) {
        return ApiResponse.ok(publicMemberAuthService.currentSession(sessionToken));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = PublicMemberAuthService.SESSION_COOKIE_NAME, required = false) String sessionToken,
            HttpServletResponse response
    ) {
        publicMemberAuthService.logout(sessionToken);
        response.addHeader("Set-Cookie", publicMemberAuthService.clearSessionCookie().toString());
        return ApiResponse.ok(null);
    }
}
