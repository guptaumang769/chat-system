package com.umang.chat.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Receives cross-node envelopes from Redis pub/sub and delivers to locally-connected recipients. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossNodeMessageSubscriber {

    private final LocalSessionRegistry localSessions;
    private final WebSocketDeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    public void onMessage(String json) {
        try {
            CrossNodeEnvelope envelope = objectMapper.readValue(json, CrossNodeEnvelope.class);
            if (!localSessions.isLocal(envelope.recipientId())) {
                return; // Not our connection — some other node owns it (or nobody does).
            }
            switch (envelope.kind()) {
                case MESSAGE -> deliveryService.pushMessageLocally(
                        envelope.recipientId(), envelope.message());
                case RECEIPT -> deliveryService.pushReceiptLocally(
                        envelope.recipientId(), envelope.receipt());
            }
        } catch (Exception e) {
            log.error("Failed to handle cross-node delivery envelope: {}", e.toString());
        }
    }
}
