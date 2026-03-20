package com.nucosmos.pos.backend.auth;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final PinAuthService pinAuthService;

    public AuthController(PinAuthService pinAuthService) {
        this.pinAuthService = pinAuthService;
    }

    @PostMapping("/pin-login")
    public ApiResponse<PinLoginResponse> pinLogin(@Valid @RequestBody PinLoginRequest request) {
        return ApiResponse.ok(pinAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentSessionResponse> me(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(pinAuthService.currentSession(user));
    }
}
