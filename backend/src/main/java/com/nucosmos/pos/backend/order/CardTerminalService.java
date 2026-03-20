package com.nucosmos.pos.backend.order;

public interface CardTerminalService {

    CardTerminalProvider provider();

    CardTransactionResult authorize(CardAuthorizationCommand command);

    CardTransactionResult capture(CardCaptureCommand command);

    CardTransactionResult voidTransaction(CardVoidCommand command);

    CardTransactionResult refund(CardRefundCommand command);
}
