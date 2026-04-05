package com.nucosmos.pos.backend.space;

public record PublicSpaceMemberBookingResponse(
        String id,
        String bookingNumber,
        String status,
        String spaceSlug,
        String spaceName,
        String eventName,
        String eventLink,
        int attendeeCount,
        String note,
        String startAt,
        String endAt,
        boolean canEdit,
        boolean canCancel
) {
}
