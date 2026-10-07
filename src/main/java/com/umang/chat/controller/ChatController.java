package com.umang.chat.controller;

import com.umang.chat.dto.request.AckRequest;
import com.umang.chat.dto.request.SendMessageRequest;
import com.umang.chat.dto.response.MessageResponse;
import com.umang.chat.dto.response.ReceiptEvent;
import com.umang.chat.model.entity.Message;
import com.umang.chat.service.ChatService;
import com.umang.chat.ws.WebSocketDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/** STOMP message handlers for sending messages and acknowledging delivery/read. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final WebSocketDeliveryService deliveryService;

    @MessageMapping("chat.send")
    public void send(@Valid @Payload SendMessageRequest request) {
        MessageResponse stored = chatService.sendMessage(request);
        log.debug("Message {} accepted for conversation {}",
                stored.getId(), stored.getConversationId());
    }

    @MessageMapping("chat.ack")
    public void ack(@Valid @Payload AckRequest request) {
        Message updated = chatService.updateStatus(request.getMessageId(), request.getStatus());
        deliveryService.deliverReceipt(updated.getSenderId(), ReceiptEvent.builder()
                .messageId(updated.getId())
                .conversationId(updated.getConversationId())
                .status(updated.getStatus())
                .build());
    }
}
