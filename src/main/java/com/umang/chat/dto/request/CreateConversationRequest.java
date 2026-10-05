package com.umang.chat.dto.request;

import com.umang.chat.model.enums.ConversationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
public class CreateConversationRequest {

    @NotNull
    private ConversationType type;

    /** Group name; ignored for ONE_TO_ONE. */
    private String name;

    /** All participant user ids (including the creator). */
    @NotEmpty
    private List<Long> memberIds;
}
