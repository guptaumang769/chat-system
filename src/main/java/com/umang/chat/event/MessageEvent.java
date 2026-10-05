package com.umang.chat.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.umang.chat.model.enums.MessageStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Kafka event payload for message fan-out. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEvent {

    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String content;
    private MessageStatus status;
    private Instant createdAt;

    @JsonIgnore
    public String getEventType() {
        return "MESSAGE_SENT";
    }
}
