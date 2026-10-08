package com.umang.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.dto.response.ReceiptEvent;
import com.umang.chat.model.enums.MessageStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrossNodeMessageSubscriberTest {

    @Mock private LocalSessionRegistry localSessions;
    @Mock private WebSocketDeliveryService deliveryService;

    private ObjectMapper objectMapper;
    private CrossNodeMessageSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        subscriber = new CrossNodeMessageSubscriber(localSessions, deliveryService, objectMapper);
    }

    @Test
    void onMessage_deliversMessageLocally_whenRecipientIsLocal() throws Exception {
        MessageResponse msg = MessageResponse.builder()
                .id(1L).conversationId(10L).senderId(5L)
                .content("hi").status(MessageStatus.SENT).createdAt(Instant.now())
                .build();
        CrossNodeEnvelope envelope = new CrossNodeEnvelope(
                CrossNodeEnvelope.Kind.MESSAGE, 2L, msg, null);
        String json = objectMapper.writeValueAsString(envelope);

        when(localSessions.isLocal(2L)).thenReturn(true);

        subscriber.onMessage(json);

        ArgumentCaptor<MessageResponse> captor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(deliveryService).pushMessageLocally(eq(2L), captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getContent()).isEqualTo("hi");
    }

    @Test
    void onMessage_deliversReceiptLocally_whenRecipientIsLocal() throws Exception {
        ReceiptEvent receipt = ReceiptEvent.builder()
                .messageId(1L).conversationId(10L).status(MessageStatus.DELIVERED).build();
        CrossNodeEnvelope envelope = new CrossNodeEnvelope(
                CrossNodeEnvelope.Kind.RECEIPT, 5L, null, receipt);
        String json = objectMapper.writeValueAsString(envelope);

        when(localSessions.isLocal(5L)).thenReturn(true);

        subscriber.onMessage(json);

        ArgumentCaptor<ReceiptEvent> captor = ArgumentCaptor.forClass(ReceiptEvent.class);
        verify(deliveryService).pushReceiptLocally(eq(5L), captor.capture());
        assertThat(captor.getValue().getMessageId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo(MessageStatus.DELIVERED);
    }

    @Test
    void onMessage_ignoresEnvelope_whenRecipientIsNotLocal() throws Exception {
        MessageResponse msg = MessageResponse.builder()
                .id(1L).conversationId(10L).senderId(5L)
                .content("hi").status(MessageStatus.SENT).createdAt(Instant.now())
                .build();
        CrossNodeEnvelope envelope = new CrossNodeEnvelope(
                CrossNodeEnvelope.Kind.MESSAGE, 99L, msg, null);
        String json = objectMapper.writeValueAsString(envelope);

        when(localSessions.isLocal(99L)).thenReturn(false);

        subscriber.onMessage(json);

        verify(deliveryService, never()).pushMessageLocally(any(), any());
        verify(deliveryService, never()).pushReceiptLocally(any(), any());
    }

    @Test
    void onMessage_handlesInvalidJsonGracefully() {
        subscriber.onMessage("{bad-json-no-crash");

        verify(deliveryService, never()).pushMessageLocally(any(), any());
        verify(deliveryService, never()).pushReceiptLocally(any(), any());
    }
}
