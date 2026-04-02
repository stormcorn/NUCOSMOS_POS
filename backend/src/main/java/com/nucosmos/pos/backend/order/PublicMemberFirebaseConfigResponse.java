package com.nucosmos.pos.backend.order;

public record PublicMemberFirebaseConfigResponse(
        boolean configured,
        String apiKey,
        String authDomain,
        String projectId,
        String storageBucket,
        String messagingSenderId,
        String appId,
        String measurementId
) {
}
