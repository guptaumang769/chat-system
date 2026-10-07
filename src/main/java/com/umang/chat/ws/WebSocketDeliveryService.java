package com.umang.chat.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umang.chat.config.RedisConfig;
import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.dto.response.ReceiptEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** Pushes messages/receipts to a recipient's WebSocket — locally or via Redis pub/sub cross-node. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketDeliveryService {

    public static final String MESSAGE_QUEUE = "/queue/messages";
    public static final String RECEIPT_QUEUE = "/queue/receipts";

    private final SimpMessagingTemplate messagingTemplate;
    private final LocalSessionRegistry localSessions;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${chat.node-id:node-1}")
    private String nodeId;

    public void deliverMessage(Long recipientId, MessageResponse message) {
        if (localSessions.isLocal(recipientId)) {
            pushMessageLocally(recipientId, message);
        } else {
            publishCrossNode(new CrossNodeEnvelope(
                    CrossNodeEnvelope.Kind.MESSAGE, recipientId, message, null));
        }
    }

    public void deliverReceipt(Long senderId, ReceiptEvent receipt) {
        if (localSessions.isLocal(senderId)) {
            pushReceiptLocally(senderId, receipt);
        } else {
            publishCrossNode(new CrossNodeEnvelope(
                    CrossNodeEnvelope.Kind.RECEIPT, senderId, null, receipt));
        }
    }

    void pushMessageLocally(Long recipientId, MessageResponse message) {
        messagingTemplate.convertAndSendToUser(String.valueOf(recipientId), MESSAGE_QUEUE, message);
    }

    void pushReceiptLocally(Long senderId, ReceiptEvent receipt) {
        messagingTemplate.convertAndSendToUser(String.valueOf(senderId), RECEIPT_QUEUE, receipt);
    }

    private void publishCrossNode(CrossNodeEnvelope envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            redisTemplate.convertAndSend(RedisConfig.DELIVERY_CHANNEL, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cross-node envelope for user {}: {}",
                    envelope.recipientId(), e.toString());
        }
    }
}
