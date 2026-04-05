package com.nucosmos.pos.backend.space;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/spaces")
public class PublicSpaceController {

    private final SpaceBookingService spaceBookingService;

    public PublicSpaceController(SpaceBookingService spaceBookingService) {
        this.spaceBookingService = spaceBookingService;
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
            @Valid @RequestBody PublicSpaceBookingRequest request
    ) {
        return ApiResponse.ok(spaceBookingService.createPublicBookingRequest(slug, request));
    }
}
