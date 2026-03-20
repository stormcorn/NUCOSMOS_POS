package com.nucosmos.pos.backend.order;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MockCardTerminalService implements CardTerminalService {

    @Override
    public CardTerminalProvider provider() {
        return CardTerminalProvider.TCB_MOCK;
    }

    @Override
    public CardTransactionResult authorize(CardAuthorizationCommand command) {
        String token = token();
        return buildResult(token, CardTransactionStatus.AUTHORIZED);
    }

    @Override
    public CardTransactionResult capture(CardCaptureCommand command) {
        String token = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return buildResult(token, CardTransactionStatus.CAPTURED);
    }

    @Override
    public CardTransactionResult voidTransaction(CardVoidCommand command) {
        String token = command.terminalTransactionId() != null ? command.terminalTransactionId().replace("-", "") : token();
        return buildResult(token, CardTransactionStatus.VOIDED);
    }

    @Override
    public CardTransactionResult refund(CardRefundCommand command) {
        String token = command.payment().getCardTerminalTxnId() != null ? command.payment().getCardTerminalTxnId().replace("-", "") : token();
        return buildResult(token, CardTransactionStatus.REFUNDED);
    }

    private String token() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private CardTransactionResult buildResult(String token, CardTransactionStatus status) {
        String normalized = (token + "000000000000000000000000").substring(0, 24);
        return new CardTransactionResult(
                provider(),
                status,
                "TCB-" + normalized.substring(0, 12),
                normalized.substring(0, 6),
                "4111********1111",
                normalized.substring(6, 10),
                normalized.substring(10, 22),
                "CONTACT"
        );
    }
}
