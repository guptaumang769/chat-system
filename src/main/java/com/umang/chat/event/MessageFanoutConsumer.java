package com.umang.chat.event;

import com.umang.chat.config.KafkaConfig;
import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.dto.response.ReceiptEvent;
import com.umang.chat.model.entity.Message;
import com.umang.chat.model.enums.MessageStatus;
import com.umang.chat.service.ChatService;
import com.umang.chat.service.PresenceService;
import com.umang.chat.ws.WebSocketDeliveryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Kafka consumer that fans out each message to online recipients via WebSocket. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageFanoutConsumer {

    private final ChatService chatService;
    private final PresenceService presenceService;
    private final WebSocketDeliveryService deliveryService;

    @KafkaListener(topics = KafkaConfig.CHAT_MESSAGES_TOPIC,
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(MessageEvent event) {
        MessageResponse payload = MessageResponse.builder()
                .id(event.getMessageId())
                .conversationId(event.getConversationId())
                .senderId(event.getSenderId())
                .content(event.getContent())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .build();

        List<Long> recipients = chatService.recipientsOf(event.getConversationId(), event.getSenderId());
        boolean deliveredToAnyone = false;

        for (Long recipientId : recipients) {
            if (presenceService.isOnline(recipientId)) {
                deliveryService.deliverMessage(recipientId, payload);
                deliveredToAnyone = true;
            }
        }

        if (deliveredToAnyone) {
            Message updated = chatService.updateStatus(event.getMessageId(), MessageStatus.DELIVERED);
            deliveryService.deliverReceipt(event.getSenderId(), ReceiptEvent.builder()
                    .messageId(updated.getId())
                    .conversationId(updated.getConversationId())
                    .status(updated.getStatus())
                    .build());
        }
    }

    @KafkaListener(topics = KafkaConfig.CHAT_MESSAGES_DLT,
            containerFactory = "kafkaListenerContainerFactory")
    public void onDeadLetter(MessageEvent event) {
        log.error("Dead-lettered message event {} for conversation {} — needs manual inspection",
                event.getMessageId(), event.getConversationId());
    }
}
