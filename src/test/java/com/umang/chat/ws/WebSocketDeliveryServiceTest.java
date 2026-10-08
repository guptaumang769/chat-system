package com.umang.chat.ws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.umang.chat.config.RedisConfig;
import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.dto.response.ReceiptEvent;
import com.umang.chat.model.enums.MessageStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class WebSocketDeliveryServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private LocalSessionRegistry localSessions;
    @Mock private RedisTemplate<String, String> redisTemplate;

    private ObjectMapper objectMapper;
    private WebSocketDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        deliveryService = new WebSocketDeliveryService(
                messagingTemplate, localSessions, redisTemplate, objectMapper);
    }

    private MessageResponse sampleMessage() {
        return MessageResponse.builder()
                .id(1L).conversationId(10L).senderId(5L)
                .content("hi").status(MessageStatus.SENT).createdAt(Instant.now())
                .build();
    }

    @Test
    void deliverMessage_sendsLocally_whenRecipientIsLocal() {
        when(localSessions.isLocal(2L)).thenReturn(true);

        deliveryService.deliverMessage(2L, sampleMessage());

        verify(messagingTemplate).convertAndSendToUser(
                eq("2"), eq(WebSocketDeliveryService.MESSAGE_QUEUE), any(MessageResponse.class));
    }

    @Test
    void deliverMessage_publishesCrossNode_whenRecipientIsRemote() {
        when(localSessions.isLocal(2L)).thenReturn(false);

        deliveryService.deliverMessage(2L, sampleMessage());

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
        verify(redisTemplate).convertAndSend(eq(RedisConfig.DELIVERY_CHANNEL), any(String.class));
    }

    @Test
    void deliverReceipt_sendsLocally_whenSenderIsLocal() {
        when(localSessions.isLocal(5L)).thenReturn(true);

        ReceiptEvent receipt = ReceiptEvent.builder()
                .messageId(1L).conversationId(10L).status(MessageStatus.DELIVERED).build();

        deliveryService.deliverReceipt(5L, receipt);

        verify(messagingTemplate).convertAndSendToUser(
                eq("5"), eq(WebSocketDeliveryService.RECEIPT_QUEUE), any(ReceiptEvent.class));
    }

    @Test
    void deliverReceipt_publishesCrossNode_whenSenderIsRemote() {
        when(localSessions.isLocal(5L)).thenReturn(false);

        ReceiptEvent receipt = ReceiptEvent.builder()
                .messageId(1L).conversationId(10L).status(MessageStatus.DELIVERED).build();

        deliveryService.deliverReceipt(5L, receipt);

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
        verify(redisTemplate).convertAndSend(eq(RedisConfig.DELIVERY_CHANNEL), any(String.class));
    }
}
