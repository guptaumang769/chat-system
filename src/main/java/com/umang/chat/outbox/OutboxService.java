package com.umang.chat.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umang.chat.event.MessageEvent;
import com.umang.chat.model.entity.OutboxEvent;
import com.umang.chat.repository.OutboxEventRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Serializes a MessageEvent and persists it as an outbox row within the caller's transaction. */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public void record(MessageEvent event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
        OutboxEvent row = OutboxEvent.builder()
                .aggregateId(String.valueOf(event.getConversationId()))
                .eventType(event.getEventType())
                .payload(payload)
                .published(false)
                .createdAt(Instant.now())
                .build();
        repository.save(row);
    }
}
