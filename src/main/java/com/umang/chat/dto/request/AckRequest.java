package com.umang.chat.dto.request;

import com.umang.chat.model.enums.MessageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Receipt acknowledgement sent by a recipient over {@code /app/chat.ack}. {@code status}
 * is the new state being reported — DELIVERED when the device received the push, READ when
 * the user opened the conversation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AckRequest {

    @NotNull
    private Long messageId;

    /** The user reporting the receipt (a recipient, not the original sender). */
    @NotNull
    private Long userId;

    @NotNull
    private MessageStatus status;
}
