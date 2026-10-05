package com.umang.chat.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Presence view: whether a user is currently online, and their last-seen instant if offline. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresenceResponse {

    private Long userId;
    private boolean online;
    private Instant lastSeen;
}
