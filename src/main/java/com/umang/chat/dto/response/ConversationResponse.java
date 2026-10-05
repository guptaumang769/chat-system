package com.umang.chat.dto.response;

import com.umang.chat.model.enums.ConversationType;
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
public class ConversationResponse {

    private Long id;
    private ConversationType type;
    private String name;
    private List<Long> memberIds;
}
