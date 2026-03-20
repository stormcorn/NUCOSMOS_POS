package com.nucosmos.pos.backend.order;

public enum CardTransactionStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    VOIDED,
    REFUNDED,
    FAILED;
}
