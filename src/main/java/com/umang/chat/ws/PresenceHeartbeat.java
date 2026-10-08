package com.umang.chat.ws;

import com.umang.chat.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically refreshes the Redis TTL for every locally-connected user so they stay online. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceHeartbeat {

    private final LocalSessionRegistry localSessions;
    private final PresenceService presenceService;

    @Value("${chat.node-id:node-1}")
    private String nodeId;

    @Scheduled(fixedDelayString = "${chat.presence.heartbeat-ms:20000}")
    public void refresh() {
        for (Long userId : localSessions.getLocalUsers()) {
            presenceService.markOnline(userId, nodeId);
        }
    }
}
