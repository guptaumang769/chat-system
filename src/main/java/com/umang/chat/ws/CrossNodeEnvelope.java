package com.umang.chat.ws;

import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.dto.response.ReceiptEvent;

/** Payload published to the Redis cross-node delivery channel. */
public record CrossNodeEnvelope(
        Kind kind,
        Long recipientId,
        MessageResponse message,
        ReceiptEvent receipt) {

    public enum Kind {
        MESSAGE,
        RECEIPT
    }
}
