package com.nucosmos.pos.backend.space;

public record PublicSpaceUpcomingEventResponse(
        String spaceSlug,
        String spaceName,
        String locationLabel,
        String eventName,
        String eventLink,
        String startAt,
        String endAt
) {
}
