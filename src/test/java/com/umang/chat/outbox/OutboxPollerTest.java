package com.umang.chat.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.umang.chat.config.KafkaConfig;
import com.umang.chat.event.MessageEvent;
import com.umang.chat.model.entity.OutboxEvent;
import com.umang.chat.model.enums.MessageStatus;
import com.umang.chat.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock private OutboxEventRepository repository;
    @Mock private KafkaTemplate<String, MessageEvent> kafkaTemplate;

    private ObjectMapper objectMapper;
    private OutboxPoller poller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        poller = new OutboxPoller(repository, kafkaTemplate, objectMapper);
    }

    @Test
    void relay_publishesUnpublishedEventsToKafka_andMarksPublished() throws Exception {
        MessageEvent event = MessageEvent.builder()
                .messageId(1L).conversationId(10L).senderId(5L)
                .content("hi").status(MessageStatus.SENT).createdAt(Instant.now())
                .build();
        OutboxEvent row = OutboxEvent.builder()
                .id(100L)
                .aggregateId("10")
                .eventType("MESSAGE_SENT")
                .payload(objectMapper.writeValueAsString(event))
                .published(false)
                .createdAt(Instant.now())
                .build();

        when(repository.findByPublishedFalseOrderByCreatedAtAsc(any(Limit.class)))
                .thenReturn(List.of(row));

        poller.relay();

        ArgumentCaptor<MessageEvent> sentEvent = ArgumentCaptor.forClass(MessageEvent.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.CHAT_MESSAGES_TOPIC), eq("10"), sentEvent.capture());
        assertThat(sentEvent.getValue().getMessageId()).isEqualTo(1L);

        assertThat(row.isPublished()).isTrue();
        assertThat(row.getPublishedAt()).isNotNull();
        verify(repository).saveAll(List.of(row));
    }

    @Test
    void relay_doesNothingWhenNoPendingEvents() {
        when(repository.findByPublishedFalseOrderByCreatedAtAsc(any(Limit.class)))
                .thenReturn(List.of());

        poller.relay();

        verify(kafkaTemplate, never()).send(any(), any(), any(MessageEvent.class));
        verify(repository, never()).saveAll(any());
    }
}
