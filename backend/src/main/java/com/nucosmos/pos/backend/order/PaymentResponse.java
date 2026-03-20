package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String paymentMethod,
        String status,
        BigDecimal amount,
        BigDecimal amountReceived,
        BigDecimal changeAmount,
        String cardTerminalProvider,
        String cardTransactionStatus,
        String cardTerminalTransactionId,
        String cardApprovalCode,
        String cardMaskedPan,
        String cardBatchNumber,
        String cardRetrievalReferenceNumber,
        String cardEntryMode,
        OffsetDateTime authorizedAt,
        OffsetDateTime capturedAt,
        OffsetDateTime voidedAt,
        OffsetDateTime refundedAt,
        String createdByEmployeeCode,
        String note,
        OffsetDateTime paidAt
) {
}
