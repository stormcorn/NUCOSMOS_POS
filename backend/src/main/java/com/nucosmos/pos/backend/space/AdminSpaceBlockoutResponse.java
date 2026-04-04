package com.nucosmos.pos.backend.space;

public record AdminSpaceBlockoutResponse(
        String id,
        String spaceId,
        String spaceName,
        String title,
        String reason,
        String startAt,
        String endAt,
        String createdBy
) {
}
