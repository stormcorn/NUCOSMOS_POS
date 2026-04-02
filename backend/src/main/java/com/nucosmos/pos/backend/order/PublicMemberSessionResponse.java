package com.nucosmos.pos.backend.order;

public record PublicMemberSessionResponse(
        boolean authenticated,
        ReceiptMemberSummary member,
        boolean deviceTrusted
) {
}
