package com.nucosmos.pos.backend.space;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import com.nucosmos.pos.backend.order.PublicMemberAuthService;
import com.nucosmos.pos.backend.order.persistence.ReceiptMemberEntity;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/spaces")
public class PublicSpaceController {

    private final SpaceBookingService spaceBookingService;
    private final PublicMemberAuthService publicMemberAuthService;

    public PublicSpaceController(
            SpaceBookingService spaceBookingService,
            PublicMemberAuthService publicMemberAuthService
    ) {
        this.spaceBookingService = spaceBookingService;
        this.publicMemberAuthService = publicMemberAuthService;
    }

    @GetMapping
    public ApiResponse<List<PublicSpaceResourceResponse>> listSpaces() {
        return ApiResponse.ok(spaceBookingService.listPublicSpaces());
    }

    @GetMapping("/{slug}")
    public ApiResponse<PublicSpaceResourceResponse> getSpace(@PathVariable String slug) {
        return ApiResponse.ok(spaceBookingService.getPublicSpace(slug));
    }

    @GetMapping("/{slug}/availability")
    public ApiResponse<PublicSpaceAvailabilityResponse> availability(
            @PathVariable String slug,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.ok(spaceBookingService.getPublicAvailability(slug, from, to));
    }

    @GetMapping("/upcoming-events")
    public ApiResponse<List<PublicSpaceUpcomingEventResponse>> upcomingEvents(
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ApiResponse.ok(spaceBookingService.listPublicUpcomingEvents(limit));
    }

    @PostMapping("/{slug}/booking-requests")
    public ApiResponse<PublicSpaceBookingResponse> createBookingRequest(
            @PathVariable String slug,
            @Valid @RequestBody PublicSpaceBookingRequest request,
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
        ReceiptMemberEntity member = authenticated == null ? null : authenticated.member();
        return ApiResponse.ok(spaceBookingService.createPublicBookingRequest(slug, request, member));
    }

    @GetMapping("/member/bookings")
    public ApiResponse<List<PublicSpaceMemberBookingResponse>> memberBookings(
            @CookieValue(value = PublicMemberAuthService.SESSION_COOKIE_NAME, required = false) String sessionToken,
            @RequestHeader(value = "X-Nucosmos-Device-Token", required = false) String deviceToken,
            @RequestHeader(value = "X-Nucosmos-Device-Label", required = false) String deviceLabel,
            HttpServletResponse response
    ) {
        ReceiptMemberEntity member = requireAuthenticatedMember(sessionToken, deviceToken, deviceLabel, response);
        return ApiResponse.ok(spaceBookingService.listPublicMemberBookings(member));
    }

    @PatchMapping("/member/bookings/{bookingId}")
    public ApiResponse<PublicSpaceMemberBookingResponse> updateMemberBooking(
            @PathVariable java.util.UUID bookingId,
            @Valid @RequestBody PublicSpaceMemberBookingUpdateRequest request,
            @CookieValue(value = PublicMemberAuthService.SESSION_COOKIE_NAME, required = false) String sessionToken,
            @RequestHeader(value = "X-Nucosmos-Device-Token", required = false) String deviceToken,
            @RequestHeader(value = "X-Nucosmos-Device-Label", required = false) String deviceLabel,
            HttpServletResponse response
    ) {
        ReceiptMemberEntity member = requireAuthenticatedMember(sessionToken, deviceToken, deviceLabel, response);
        return ApiResponse.ok(spaceBookingService.updatePublicMemberBooking(member, bookingId, request));
    }

    @PostMapping("/member/bookings/{bookingId}/cancel")
    public ApiResponse<PublicSpaceMemberBookingResponse> cancelMemberBooking(
            @PathVariable java.util.UUID bookingId,
            @CookieValue(value = PublicMemberAuthService.SESSION_COOKIE_NAME, required = false) String sessionToken,
            @RequestHeader(value = "X-Nucosmos-Device-Token", required = false) String deviceToken,
            @RequestHeader(value = "X-Nucosmos-Device-Label", required = false) String deviceLabel,
            HttpServletResponse response
    ) {
        ReceiptMemberEntity member = requireAuthenticatedMember(sessionToken, deviceToken, deviceLabel, response);
        return ApiResponse.ok(spaceBookingService.cancelPublicMemberBooking(member, bookingId));
    }

    private ReceiptMemberEntity requireAuthenticatedMember(
            String sessionToken,
            String deviceToken,
            String deviceLabel,
            HttpServletResponse response
    ) {
        PublicMemberAuthService.AuthenticatedMemberResult authenticated = publicMemberAuthService.resolveAuthenticatedMember(
                sessionToken,
                deviceToken,
                deviceLabel
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Member login is required"));
        if (authenticated.sessionCookie() != null) {
            response.addHeader("Set-Cookie", authenticated.sessionCookie().toString());
        }
        return authenticated.member();
    }
}
