package com.nucosmos.pos.backend.shift;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shifts")
@PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'ADMIN')")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping("/current")
    public ApiResponse<ShiftResponse> currentShift(Authentication authentication) {
        return ApiResponse.ok(shiftService.currentShift(requireUser(authentication)));
    }

    @PostMapping("/open")
    public ApiResponse<ShiftResponse> openShift(
            Authentication authentication,
            @Valid @RequestBody ShiftOpenRequest request
    ) {
        return ApiResponse.ok(shiftService.openShift(requireUser(authentication), request));
    }

    @PostMapping("/{shiftId}/close")
    public ApiResponse<ShiftResponse> closeShift(
            @PathVariable UUID shiftId,
            Authentication authentication,
            @Valid @RequestBody ShiftCloseRequest request
    ) {
        return ApiResponse.ok(shiftService.closeShift(shiftId, requireUser(authentication), request));
    }

    private AuthenticatedUser requireUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
