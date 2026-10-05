package com.umang.chat.dto.response;

import com.umang.chat.model.entity.Message;
import com.umang.chat.model.enums.MessageStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private MessageStatus status;
    private Instant createdAt;

    public static MessageResponse from(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
