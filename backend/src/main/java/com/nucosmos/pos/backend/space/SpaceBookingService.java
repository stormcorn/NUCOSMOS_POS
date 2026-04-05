package com.nucosmos.pos.backend.space;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.auth.persistence.UserEntity;
import com.nucosmos.pos.backend.auth.repository.UserRepository;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.space.persistence.SpaceBlockoutEntity;
import com.nucosmos.pos.backend.space.persistence.SpaceBookingEntity;
import com.nucosmos.pos.backend.space.persistence.SpaceBookingPolicyEntity;
import com.nucosmos.pos.backend.space.persistence.SpaceResourceEntity;
import com.nucosmos.pos.backend.space.repository.SpaceBlockoutRepository;
import com.nucosmos.pos.backend.space.repository.SpaceBookingRepository;
import com.nucosmos.pos.backend.space.repository.SpaceResourceRepository;
import com.nucosmos.pos.backend.store.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SpaceBookingService {

    private static final Set<String> ACTIVE_BOOKING_STATUSES = Set.of("PENDING", "CONFIRMED");

    private final SpaceResourceRepository spaceResourceRepository;
    private final SpaceBookingRepository spaceBookingRepository;
    private final SpaceBlockoutRepository spaceBlockoutRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public SpaceBookingService(
            SpaceResourceRepository spaceResourceRepository,
            SpaceBookingRepository spaceBookingRepository,
            SpaceBlockoutRepository spaceBlockoutRepository,
            StoreRepository storeRepository,
            UserRepository userRepository
    ) {
        this.spaceResourceRepository = spaceResourceRepository;
        this.spaceBookingRepository = spaceBookingRepository;
        this.spaceBlockoutRepository = spaceBlockoutRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    public List<PublicSpaceResourceResponse> listPublicSpaces() {
        return spaceResourceRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toPublicSpace)
                .toList();
    }

    public PublicSpaceResourceResponse getPublicSpace(String slug) {
        return toPublicSpace(requireActiveSpace(slug));
    }

    public PublicSpaceAvailabilityResponse getPublicAvailability(String slug, LocalDate from, LocalDate to) {
        SpaceResourceEntity space = requireActiveSpace(slug);
        SpaceBookingPolicyEntity policy = requirePolicy(space);

        LocalDate effectiveFrom = from != null ? from : LocalDate.now();
        LocalDate effectiveTo = to != null ? to : effectiveFrom.plusDays(13);
        validateDateRange(effectiveFrom, effectiveTo);

        ZoneId zoneId = ZoneId.of(space.getStore().getTimezone());
        OffsetDateTime rangeStart = atZone(effectiveFrom, policy.getDefaultOpenTime(), zoneId);
        OffsetDateTime rangeEnd = atZone(effectiveTo.plusDays(1), policy.getDefaultCloseTime(), zoneId);

        List<SpaceBookingEntity> bookings = spaceBookingRepository
                .findAllBySpaceResource_IdAndEndAtAfterAndStartAtBeforeAndStatusInOrderByStartAtAsc(
                        space.getId(),
                        rangeStart,
                        rangeEnd,
                        ACTIVE_BOOKING_STATUSES
                );
        List<SpaceBlockoutEntity> blockouts = spaceBlockoutRepository
                .findAllBySpaceResource_IdAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(
                        space.getId(),
                        rangeStart,
                        rangeEnd
                );

        List<PublicSpaceAvailabilityDayResponse> days = new ArrayList<>();
        for (LocalDate current = effectiveFrom; !current.isAfter(effectiveTo); current = current.plusDays(1)) {
            days.add(new PublicSpaceAvailabilityDayResponse(
                    current.toString(),
                    buildSlotsForDay(current, space, policy, zoneId, bookings, blockouts)
            ));
        }

        return new PublicSpaceAvailabilityResponse(
                space.getSlug(),
                space.getName(),
                space.getStore().getTimezone(),
                policy.getBookingIntervalMinutes(),
                days
        );
    }

    public PublicSpaceBookingResponse createPublicBookingRequest(String slug, PublicSpaceBookingRequest request) {
        SpaceResourceEntity space = requireActiveSpace(slug);
        SpaceBookingEntity booking = createBooking(
                space,
                request.customerName(),
                request.customerPhone(),
                request.customerEmail(),
                request.purpose(),
                request.attendeeCount(),
                request.note(),
                null,
                request.startAt(),
                request.endAt(),
                "OFFICIAL_WEB",
                "PENDING"
        );
        spaceBookingRepository.save(booking);
        return new PublicSpaceBookingResponse(
                booking.getBookingNumber(),
                booking.getStatus(),
                booking.getSpaceResource().getName(),
                booking.getStartAt().toString(),
                booking.getEndAt().toString(),
                booking.getSubtotalAmount(),
                booking.getBalanceAmount(),
                "Booking request received. The venue team will review this slot."
        );
    }

    public List<AdminSpaceResourceResponse> listAdminSpaces(AuthenticatedUser user) {
        ensureActiveStore(user.storeCode());
        return spaceResourceRepository.findAllByStore_CodeAndActiveTrueOrderByNameAsc(user.storeCode()).stream()
                .map(this::toAdminSpace)
                .toList();
    }

    public List<AdminSpaceBookingSummaryResponse> listAdminBookings(
            AuthenticatedUser user,
            String status,
            LocalDate from,
            LocalDate to
    ) {
        ensureActiveStore(user.storeCode());
        OffsetDateTime fromDateTime = atStartOfDay(from != null ? from : LocalDate.now().minusDays(7));
        OffsetDateTime toDateTime = atStartOfDay((to != null ? to : LocalDate.now().plusDays(30)).plusDays(1));

        List<SpaceBookingEntity> bookings = status == null || status.isBlank()
                ? spaceBookingRepository.findAllBySpaceResource_Store_CodeAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(
                        user.storeCode(), fromDateTime, toDateTime)
                : spaceBookingRepository.findAllBySpaceResource_Store_CodeAndStatusAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(
                        user.storeCode(), status.trim(), fromDateTime, toDateTime);

        return bookings.stream().map(this::toAdminSummary).toList();
    }

    public AdminSpaceBookingResponse getAdminBooking(AuthenticatedUser user, UUID bookingId) {
        return toAdminResponse(requireBooking(user.storeCode(), bookingId));
    }

    public AdminSpaceBookingResponse createManualBooking(AuthenticatedUser user, AdminSpaceBookingRequest request) {
        SpaceResourceEntity space = requireAdminSpace(user.storeCode(), request.spaceResourceId());
        String source = request.source() == null || request.source().isBlank() ? "MANUAL" : request.source().trim();
        String status = request.status() == null || request.status().isBlank() ? "CONFIRMED" : request.status().trim();

        SpaceBookingEntity booking = createBooking(
                space,
                request.customerName(),
                request.customerPhone(),
                request.customerEmail(),
                request.purpose(),
                request.attendeeCount(),
                request.note(),
                request.internalNote(),
                request.startAt(),
                request.endAt(),
                source,
                status
        );
        if ("CONFIRMED".equals(status)) {
            booking.approve(requireUser(user.userId()), request.internalNote());
        }
        return saveAndReloadBooking(user.storeCode(), booking);
    }

    public AdminSpaceBookingResponse approveBooking(
            AuthenticatedUser user,
            UUID bookingId,
            AdminSpaceBookingDecisionRequest request
    ) {
        SpaceBookingEntity booking = requireBooking(user.storeCode(), bookingId);
        ensureBookingCanStayActive(booking.getSpaceResource(), booking.getStartAt(), booking.getEndAt(), booking.getId());
        booking.approve(requireUser(user.userId()), request.internalNote());
        return saveAndReloadBooking(user.storeCode(), booking);
    }

    public AdminSpaceBookingResponse rejectBooking(
            AuthenticatedUser user,
            UUID bookingId,
            AdminSpaceBookingDecisionRequest request
    ) {
        SpaceBookingEntity booking = requireBooking(user.storeCode(), bookingId);
        booking.reject(request.internalNote());
        return saveAndReloadBooking(user.storeCode(), booking);
    }

    public AdminSpaceBookingResponse cancelBooking(
            AuthenticatedUser user,
            UUID bookingId,
            AdminSpaceBookingDecisionRequest request
    ) {
        SpaceBookingEntity booking = requireBooking(user.storeCode(), bookingId);
        booking.cancel(request.internalNote());
        return saveAndReloadBooking(user.storeCode(), booking);
    }

    private AdminSpaceBookingResponse saveAndReloadBooking(String storeCode, SpaceBookingEntity booking) {
        SpaceBookingEntity savedBooking = spaceBookingRepository.save(booking);
        return toAdminResponse(requireBooking(storeCode, savedBooking.getId()));
    }

    public List<AdminSpaceBlockoutResponse> listBlockouts(AuthenticatedUser user, LocalDate from, LocalDate to) {
        ensureActiveStore(user.storeCode());
        OffsetDateTime fromDateTime = atStartOfDay(from != null ? from : LocalDate.now().minusDays(7));
        OffsetDateTime toDateTime = atStartOfDay((to != null ? to : LocalDate.now().plusDays(30)).plusDays(1));
        return spaceBlockoutRepository.findAllBySpaceResource_Store_CodeAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(
                        user.storeCode(), fromDateTime, toDateTime)
                .stream()
                .map(this::toBlockoutResponse)
                .toList();
    }

    public AdminSpaceBlockoutResponse createBlockout(AuthenticatedUser user, AdminSpaceBlockoutRequest request) {
        SpaceResourceEntity space = requireAdminSpace(user.storeCode(), request.spaceResourceId());
        validateWindow(space, request.startAt(), request.endAt());
        ensureNoActiveBookingOverlap(space, request.startAt(), request.endAt(), null);

        SpaceBlockoutEntity blockout = SpaceBlockoutEntity.create(
                space,
                requireUser(user.userId()),
                request.title().trim(),
                blankToNull(request.reason()),
                request.startAt(),
                request.endAt()
        );
        return toBlockoutResponse(spaceBlockoutRepository.save(blockout));
    }

    public void deleteBlockout(AuthenticatedUser user, UUID blockoutId) {
        SpaceBlockoutEntity blockout = spaceBlockoutRepository.findByIdAndSpaceResource_Store_Code(blockoutId, user.storeCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blockout not found"));
        spaceBlockoutRepository.delete(blockout);
    }

    private SpaceBookingEntity createBooking(
            SpaceResourceEntity space,
            String customerName,
            String customerPhone,
            String customerEmail,
            String purpose,
            int attendeeCount,
            String note,
            String internalNote,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String source,
            String status
    ) {
        validateWindow(space, startAt, endAt);
        validateAttendees(space, attendeeCount);
        ensureBookingCanStayActive(space, startAt, endAt, null);

        long minutes = Duration.between(startAt, endAt).toMinutes();
        BigDecimal subtotal = requirePolicy(space).getHourlyRate()
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        return SpaceBookingEntity.create(
                space,
                generateBookingNumber(space.getStore().getCode(), startAt),
                status,
                source,
                customerName.trim(),
                customerPhone.trim(),
                blankToNull(customerEmail),
                blankToNull(purpose),
                attendeeCount,
                subtotal,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                subtotal,
                blankToNull(note),
                blankToNull(internalNote),
                startAt,
                endAt
        );
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BadRequestException("Availability range is invalid.");
        }
        if (ChronoUnit.DAYS.between(from, to) > 31) {
            throw new BadRequestException("Availability range cannot exceed 31 days.");
        }
    }

    private void validateWindow(SpaceResourceEntity space, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new BadRequestException("Booking window is invalid.");
        }

        SpaceBookingPolicyEntity policy = requirePolicy(space);
        ZoneId zoneId = ZoneId.of(space.getStore().getTimezone());
        ZonedDateTime startLocal = startAt.atZoneSameInstant(zoneId);
        ZonedDateTime endLocal = endAt.atZoneSameInstant(zoneId);

        if (!startLocal.toLocalDate().equals(endLocal.toLocalDate())) {
            throw new BadRequestException("MVP only supports single-day bookings.");
        }

        long minutes = Duration.between(startAt, endAt).toMinutes();
        if (minutes < (long) policy.getMinimumHours() * 60) {
            throw new BadRequestException("Booking does not meet the minimum duration.");
        }
        if (minutes % policy.getBookingIntervalMinutes() != 0) {
            throw new BadRequestException("Booking must align to the configured booking interval.");
        }
        if (startLocal.toLocalTime().isBefore(policy.getDefaultOpenTime())
                || endLocal.toLocalTime().isAfter(policy.getDefaultCloseTime())) {
            throw new BadRequestException("Booking falls outside venue opening hours.");
        }
    }

    private void validateAttendees(SpaceResourceEntity space, int attendeeCount) {
        if (attendeeCount <= 0) {
            throw new BadRequestException("Attendee count must be greater than zero.");
        }
        if (attendeeCount > requirePolicy(space).getMaxAttendees()) {
            throw new BadRequestException("Attendee count exceeds the venue limit.");
        }
    }

    private void ensureBookingCanStayActive(
            SpaceResourceEntity space,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            UUID excludeBookingId
    ) {
        ensureNoActiveBookingOverlap(space, startAt, endAt, excludeBookingId);
        ensureNoBlockoutOverlap(space, startAt, endAt);
    }

    private void ensureNoActiveBookingOverlap(
            SpaceResourceEntity space,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            UUID excludeBookingId
    ) {
        List<SpaceBookingEntity> overlaps = spaceBookingRepository
                .findAllBySpaceResource_IdAndEndAtAfterAndStartAtBeforeAndStatusInOrderByStartAtAsc(
                        space.getId(),
                        bufferedStart(space, startAt),
                        bufferedEnd(space, endAt),
                        ACTIVE_BOOKING_STATUSES
                )
                .stream()
                .filter(booking -> excludeBookingId == null || !booking.getId().equals(excludeBookingId))
                .toList();
        if (!overlaps.isEmpty()) {
            throw new BadRequestException("The selected time overlaps an existing booking.");
        }
    }

    private void ensureNoBlockoutOverlap(SpaceResourceEntity space, OffsetDateTime startAt, OffsetDateTime endAt) {
        List<SpaceBlockoutEntity> overlaps = spaceBlockoutRepository
                .findAllBySpaceResource_IdAndEndAtAfterAndStartAtBeforeOrderByStartAtAsc(space.getId(), startAt, endAt);
        if (!overlaps.isEmpty()) {
            throw new BadRequestException("The selected time overlaps an unavailable period.");
        }
    }

    private List<PublicSpaceAvailabilitySlotResponse> buildSlotsForDay(
            LocalDate date,
            SpaceResourceEntity space,
            SpaceBookingPolicyEntity policy,
            ZoneId zoneId,
            List<SpaceBookingEntity> bookings,
            List<SpaceBlockoutEntity> blockouts
    ) {
        List<PublicSpaceAvailabilitySlotResponse> slots = new ArrayList<>();
        ZonedDateTime cursor = ZonedDateTime.of(date, policy.getDefaultOpenTime(), zoneId);
        ZonedDateTime dayClose = ZonedDateTime.of(date, policy.getDefaultCloseTime(), zoneId);

        while (cursor.isBefore(dayClose)) {
            ZonedDateTime next = cursor.plusMinutes(policy.getBookingIntervalMinutes());
            OffsetDateTime slotStart = cursor.toOffsetDateTime();
            OffsetDateTime slotEnd = next.toOffsetDateTime();

            String status = "AVAILABLE";
            String label = "可預約";

            SpaceBlockoutEntity overlappingBlockout = blockouts.stream()
                    .filter(blockout -> overlaps(slotStart, slotEnd, blockout.getStartAt(), blockout.getEndAt()))
                    .findFirst()
                    .orElse(null);
            SpaceBookingEntity overlappingBooking = bookings.stream()
                    .filter(booking -> overlaps(slotStart, slotEnd, bufferedStart(space, booking.getStartAt()), bufferedEnd(space, booking.getEndAt())))
                    .findFirst()
                    .orElse(null);

            if (overlappingBlockout != null) {
                status = "UNAVAILABLE";
                label = overlappingBlockout.getTitle() != null && !overlappingBlockout.getTitle().isBlank()
                        ? overlappingBlockout.getTitle()
                        : "封鎖時段";
            } else if (overlappingBooking != null) {
                status = "UNAVAILABLE";
                label = overlappingBooking.getPurpose() != null && !overlappingBooking.getPurpose().isBlank()
                        ? overlappingBooking.getPurpose()
                        : "已預約";
            }

            slots.add(new PublicSpaceAvailabilitySlotResponse(slotStart.toString(), slotEnd.toString(), status, label));
            cursor = next;
        }

        return slots;
    }

    private boolean overlaps(OffsetDateTime startA, OffsetDateTime endA, OffsetDateTime startB, OffsetDateTime endB) {
        return endA.isAfter(startB) && startA.isBefore(endB);
    }

    private OffsetDateTime bufferedStart(SpaceResourceEntity space, OffsetDateTime startAt) {
        return startAt.minusMinutes(requirePolicy(space).getBufferBeforeMinutes());
    }

    private OffsetDateTime bufferedEnd(SpaceResourceEntity space, OffsetDateTime endAt) {
        return endAt.plusMinutes(requirePolicy(space).getBufferAfterMinutes());
    }

    private SpaceResourceEntity requireActiveSpace(String slug) {
        return spaceResourceRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found"));
    }

    private SpaceResourceEntity requireAdminSpace(String storeCode, UUID spaceId) {
        ensureActiveStore(storeCode);
        return spaceResourceRepository.findByIdAndStore_Code(spaceId, storeCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found"));
    }

    private SpaceBookingEntity requireBooking(String storeCode, UUID bookingId) {
        ensureActiveStore(storeCode);
        return spaceBookingRepository.findByIdAndSpaceResource_Store_Code(bookingId, storeCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void ensureActiveStore(String storeCode) {
        storeRepository.findByCodeAndStatus(storeCode, "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authenticated store is not available"));
    }

    private SpaceBookingPolicyEntity requirePolicy(SpaceResourceEntity space) {
        SpaceBookingPolicyEntity policy = space.getBookingPolicy();
        if (policy == null || !policy.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Space policy is not configured");
        }
        return policy;
    }

    private OffsetDateTime atZone(LocalDate date, LocalTime time, ZoneId zoneId) {
        return ZonedDateTime.of(date, time, zoneId).toOffsetDateTime();
    }

    private OffsetDateTime atStartOfDay(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.ofHours(8));
    }

    private String generateBookingNumber(String storeCode, OffsetDateTime startAt) {
        return "SB-" + storeCode + "-" + startAt.toLocalDate() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PublicSpaceResourceResponse toPublicSpace(SpaceResourceEntity resource) {
        SpaceBookingPolicyEntity policy = requirePolicy(resource);
        return new PublicSpaceResourceResponse(
                resource.getCode(),
                resource.getName(),
                resource.getSlug(),
                resource.getDescription(),
                resource.getLocationLabel(),
                resource.getCapacity(),
                resource.getStore().getTimezone(),
                policy.getHourlyRate(),
                policy.getCurrencyCode(),
                policy.getMinimumHours(),
                policy.getBookingIntervalMinutes(),
                policy.getBufferBeforeMinutes(),
                policy.getBufferAfterMinutes(),
                policy.getMaxAttendees(),
                policy.getCancellationPolicyText(),
                policy.getHouseRulesText()
        );
    }

    private AdminSpaceResourceResponse toAdminSpace(SpaceResourceEntity resource) {
        SpaceBookingPolicyEntity policy = requirePolicy(resource);
        return new AdminSpaceResourceResponse(
                resource.getId().toString(),
                resource.getCode(),
                resource.getName(),
                resource.getSlug(),
                resource.getLocationLabel(),
                resource.getCapacity(),
                resource.isActive(),
                resource.getStore().getTimezone(),
                policy.getHourlyRate(),
                policy.getCurrencyCode(),
                policy.getMinimumHours(),
                policy.getBookingIntervalMinutes(),
                policy.getBufferBeforeMinutes(),
                policy.getBufferAfterMinutes(),
                policy.getMaxAttendees()
        );
    }

    private AdminSpaceBookingSummaryResponse toAdminSummary(SpaceBookingEntity booking) {
        return new AdminSpaceBookingSummaryResponse(
                booking.getId().toString(),
                booking.getBookingNumber(),
                booking.getStatus(),
                booking.getSource(),
                booking.getSpaceResource().getName(),
                booking.getCustomerName(),
                booking.getCustomerPhone(),
                booking.getPurpose(),
                booking.getAttendeeCount(),
                booking.getStartAt().toString(),
                booking.getEndAt().toString(),
                booking.getSubtotalAmount(),
                booking.getApprovedAt() != null ? booking.getApprovedAt().toString() : null
        );
    }

    private AdminSpaceBookingResponse toAdminResponse(SpaceBookingEntity booking) {
        return new AdminSpaceBookingResponse(
                booking.getId().toString(),
                booking.getBookingNumber(),
                booking.getStatus(),
                booking.getSource(),
                booking.getSpaceResource().getId().toString(),
                booking.getSpaceResource().getName(),
                booking.getCustomerName(),
                booking.getCustomerPhone(),
                booking.getCustomerEmail(),
                booking.getPurpose(),
                booking.getAttendeeCount(),
                booking.getSubtotalAmount(),
                booking.getDepositAmount(),
                booking.getPaidAmount(),
                booking.getBalanceAmount(),
                booking.getNote(),
                booking.getInternalNote(),
                booking.getStartAt().toString(),
                booking.getEndAt().toString(),
                booking.getApprovedAt() != null ? booking.getApprovedAt().toString() : null,
                booking.getApprovedByUser() != null ? booking.getApprovedByUser().getDisplayName() : null,
                booking.getCancelledAt() != null ? booking.getCancelledAt().toString() : null
        );
    }

    private AdminSpaceBlockoutResponse toBlockoutResponse(SpaceBlockoutEntity blockout) {
        return new AdminSpaceBlockoutResponse(
                blockout.getId().toString(),
                blockout.getSpaceResource().getId().toString(),
                blockout.getSpaceResource().getName(),
                blockout.getTitle(),
                blockout.getReason(),
                blockout.getStartAt().toString(),
                blockout.getEndAt().toString(),
                blockout.getCreatedByUser() != null ? blockout.getCreatedByUser().getDisplayName() : null
        );
    }
}
