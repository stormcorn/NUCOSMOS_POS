package com.nucosmos.pos.backend.order;

public record CardTransactionResult(
        CardTerminalProvider provider,
        CardTransactionStatus status,
        String terminalTransactionId,
        String approvalCode,
        String maskedPan,
        String batchNumber,
        String retrievalReferenceNumber,
        String entryMode
) {
}
