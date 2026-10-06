package com.umang.chat.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umang.chat.config.KafkaConfig;
import com.umang.chat.event.MessageEvent;
import com.umang.chat.model.entity.OutboxEvent;
import com.umang.chat.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Polls unpublished outbox rows, relays them to Kafka, and marks them published. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, MessageEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.poll.fixed-delay-ms:2000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch =
                repository.findByPublishedFalseOrderByCreatedAtAsc(Limit.of(BATCH_SIZE));
        if (batch.isEmpty()) {
            return;
        }
        for (OutboxEvent row : batch) {
            try {
                MessageEvent event = objectMapper.readValue(row.getPayload(), MessageEvent.class);
                kafkaTemplate.send(KafkaConfig.CHAT_MESSAGES_TOPIC, row.getAggregateId(), event);
                row.setPublished(true);
                row.setPublishedAt(Instant.now());
            } catch (Exception e) {
                log.error("Failed to relay outbox event {} ({}): {}",
                        row.getId(), row.getEventType(), e.toString());
            }
        }
        repository.saveAll(batch);
        log.debug("Outbox poll processed {} row(s)", batch.size());
    }
}
