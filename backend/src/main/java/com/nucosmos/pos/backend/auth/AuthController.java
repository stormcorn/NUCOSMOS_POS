package com.nucosmos.pos.backend.auth;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import com.nucosmos.pos.backend.store.StoreSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final PinAuthService pinAuthService;
    private final UserPreferenceService userPreferenceService;

    public AuthController(PinAuthService pinAuthService, UserPreferenceService userPreferenceService) {
        this.pinAuthService = pinAuthService;
        this.userPreferenceService = userPreferenceService;
    }

    @PostMapping("/pin-login")
    public ApiResponse<PinLoginResponse> pinLogin(@Valid @RequestBody PinLoginRequest request) {
        return ApiResponse.ok(pinAuthService.login(request));
    }

    @GetMapping("/stores")
    public ApiResponse<List<StoreSummaryResponse>> availableStores() {
        return ApiResponse.ok(pinAuthService.listAvailableStores());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ApiResponse<CurrentSessionResponse> me(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(pinAuthService.currentSession(user));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/preferences/navigation")
    public ApiResponse<NavigationPreferenceResponse> navigationPreference(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(userPreferenceService.getNavigationPreference(user));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/preferences/navigation")
    public ApiResponse<NavigationPreferenceResponse> saveNavigationPreference(
            Authentication authentication,
            @RequestBody NavigationPreferenceRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(userPreferenceService.saveNavigationPreference(user, request));
    }
}
