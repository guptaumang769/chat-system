package com.umang.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Payload for both the STOMP {@code /app/chat.send} frame and the REST send fallback. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull
    private Long conversationId;

    @NotNull
    private Long senderId;

    @NotBlank
    private String content;
}
