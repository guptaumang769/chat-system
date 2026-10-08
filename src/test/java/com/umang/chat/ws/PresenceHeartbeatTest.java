package com.umang.chat.ws;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.umang.chat.service.PresenceService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PresenceHeartbeatTest {

    @Mock private LocalSessionRegistry localSessions;
    @Mock private PresenceService presenceService;

    private PresenceHeartbeat heartbeat;

    @BeforeEach
    void setUp() {
        heartbeat = new PresenceHeartbeat(localSessions, presenceService);
        ReflectionTestUtils.setField(heartbeat, "nodeId", "node-1");
    }

    @Test
    void refresh_reTouchesOnlineKeyForEveryLocalUser() {
        when(localSessions.getLocalUsers()).thenReturn(Set.of(1L, 2L));

        heartbeat.refresh();

        verify(presenceService).markOnline(1L, "node-1");
        verify(presenceService).markOnline(2L, "node-1");
    }

    @Test
    void refresh_doesNothingWhenNoLocalUsers() {
        when(localSessions.getLocalUsers()).thenReturn(Set.of());

        heartbeat.refresh();

        verifyNoInteractions(presenceService);
    }
}
