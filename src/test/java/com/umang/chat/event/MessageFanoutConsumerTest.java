package com.umang.chat.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.dto.response.ReceiptEvent;
import com.umang.chat.model.entity.Message;
import com.umang.chat.model.enums.MessageStatus;
import com.umang.chat.service.ChatService;
import com.umang.chat.service.PresenceService;
import com.umang.chat.ws.WebSocketDeliveryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MessageFanoutConsumerTest {

    @Mock private ChatService chatService;
    @Mock private PresenceService presenceService;
    @Mock private WebSocketDeliveryService deliveryService;

    private MessageFanoutConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new MessageFanoutConsumer(chatService, presenceService, deliveryService);
    }

    private MessageEvent sampleEvent() {
        return MessageEvent.builder()
                .messageId(10L)
                .conversationId(1L)
                .senderId(1L)
                .content("hello")
                .status(MessageStatus.SENT)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void onMessage_deliversToOnlineRecipients_andSendsDeliveryReceipt() {
        MessageEvent event = sampleEvent();
        when(chatService.recipientsOf(1L, 1L)).thenReturn(List.of(2L, 3L));
        when(presenceService.isOnline(2L)).thenReturn(true);
        when(presenceService.isOnline(3L)).thenReturn(true);

        Message updatedMessage = Message.builder()
                .id(10L).conversationId(1L).senderId(1L).content("hello")
                .status(MessageStatus.DELIVERED).createdAt(event.getCreatedAt()).build();
        when(chatService.updateStatus(10L, MessageStatus.DELIVERED)).thenReturn(updatedMessage);

        consumer.onMessage(event);

        verify(deliveryService).deliverMessage(eq(2L), any(MessageResponse.class));
        verify(deliveryService).deliverMessage(eq(3L), any(MessageResponse.class));

        ArgumentCaptor<ReceiptEvent> receipt = ArgumentCaptor.forClass(ReceiptEvent.class);
        verify(deliveryService).deliverReceipt(eq(1L), receipt.capture());
        assertThat(receipt.getValue().getStatus()).isEqualTo(MessageStatus.DELIVERED);
        assertThat(receipt.getValue().getMessageId()).isEqualTo(10L);
    }

    @Test
    void onMessage_skipsOfflineRecipients_noReceiptWhenNoneOnline() {
        MessageEvent event = sampleEvent();
        when(chatService.recipientsOf(1L, 1L)).thenReturn(List.of(2L));
        when(presenceService.isOnline(2L)).thenReturn(false);

        consumer.onMessage(event);

        verify(deliveryService, never()).deliverMessage(any(), any(MessageResponse.class));
        verify(chatService, never()).updateStatus(any(), any());
        verify(deliveryService, never()).deliverReceipt(any(), any(ReceiptEvent.class));
    }

    @Test
    void onMessage_mixedOnlineOffline_deliversOnlyToOnline() {
        MessageEvent event = sampleEvent();
        when(chatService.recipientsOf(1L, 1L)).thenReturn(List.of(2L, 3L));
        when(presenceService.isOnline(2L)).thenReturn(true);
        when(presenceService.isOnline(3L)).thenReturn(false);

        Message updatedMessage = Message.builder()
                .id(10L).conversationId(1L).senderId(1L).content("hello")
                .status(MessageStatus.DELIVERED).createdAt(event.getCreatedAt()).build();
        when(chatService.updateStatus(10L, MessageStatus.DELIVERED)).thenReturn(updatedMessage);

        consumer.onMessage(event);

        verify(deliveryService).deliverMessage(eq(2L), any(MessageResponse.class));
        verify(deliveryService, never()).deliverMessage(eq(3L), any(MessageResponse.class));
        verify(deliveryService).deliverReceipt(eq(1L), any(ReceiptEvent.class));
    }
}
