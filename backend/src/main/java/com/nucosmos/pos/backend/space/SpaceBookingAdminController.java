package com.nucosmos.pos.backend.space;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/space-bookings")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class SpaceBookingAdminController {

    private final SpaceBookingService spaceBookingService;

    public SpaceBookingAdminController(SpaceBookingService spaceBookingService) {
        this.spaceBookingService = spaceBookingService;
    }

    @GetMapping("/spaces")
    public ApiResponse<List<AdminSpaceResourceResponse>> spaces(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(spaceBookingService.listAdminSpaces(user));
    }

    @GetMapping
    public ApiResponse<List<AdminSpaceBookingSummaryResponse>> bookings(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(spaceBookingService.listAdminBookings(user, status, from, to));
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<AdminSpaceBookingResponse> booking(
            Authentication authentication,
            @PathVariable UUID bookingId
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(spaceBookingService.getAdminBooking(user, bookingId));
    }

    @PatchMapping("/{bookingId}")
    public ApiResponse<AdminSpaceBookingResponse> updateBooking(
            Authentication authentication,
            @PathVariable UUID bookingId,
            @Valid @RequestBody AdminSpaceBookingUpdateRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(spaceBookingService.updateAdminBooking(user, bookingId, request));
    }

    @PostMapping
    public ApiResponse<AdminSpaceBookingResponse> createManualBooking(
            Authentication authentication,
            @Valid @RequestBody AdminSpaceBookingRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(spaceBookingService.createManualBooking(user, request));
    }

    @PostMapping("/{bookingId}/approve")
    public ApiResponse<AdminSpaceBookingResponse> approve(
            Authentication authentication,
            @PathVariable UUID bookingId,
            @Valid @RequestBody AdminSpaceBookingDecisionRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(spaceBookingService.approveBooking(user, bookingId, request));
    }

    @PostMapping("/{bookingId}/cancel")
    public ApiResponse<AdminSpaceBookingResponse> cancel(
            Authentication authentication,
            @PathVariable UUID bookingId,
            @Valid @RequestBody AdminSpaceBookingDecisionRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(spaceBookingService.cancelBooking(user, bookingId, request));
    }

    @GetMapping("/blockouts")
    public ApiResponse<List<AdminSpaceBlockoutResponse>> blockouts(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(spaceBookingService.listBlockouts(user, from, to));
    }

    @PostMapping("/blockouts")
    public ApiResponse<AdminSpaceBlockoutResponse> createBlockout(
            Authentication authentication,
            @Valid @RequestBody AdminSpaceBlockoutRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(spaceBookingService.createBlockout(user, request));
    }

    @DeleteMapping("/blockouts/{blockoutId}")
    public ApiResponse<Boolean> deleteBlockout(
            Authentication authentication,
            @PathVariable UUID blockoutId
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        spaceBookingService.deleteBlockout(user, blockoutId);
        return ApiResponse.ok(true);
    }
}
