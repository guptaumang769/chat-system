package com.umang.chat.dto.response;

import com.umang.chat.model.enums.MessageStatus;
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
public class ReceiptEvent {

    private Long messageId;
    private Long conversationId;
    private MessageStatus status;
}
