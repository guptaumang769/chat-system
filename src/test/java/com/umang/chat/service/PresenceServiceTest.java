package com.umang.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umang.chat.dto.response.PresenceResponse;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Pure unit tests for the Redis-backed presence logic — RedisTemplate is mocked, no Docker.
 * Verifies the online marker is written with a TTL heartbeat, disconnect stamps last-seen,
 * and the presence view reports online vs offline correctly.
 */
@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService(redisTemplate);
    }

    @Test
    void markOnline_writesOnlineKeyWithTtlHeartbeat() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        presenceService.markOnline(42L, "node-1");

        verify(valueOps).set(eq("presence:42"), eq("node-1"), any(Duration.class));
    }

    @Test
    void markOffline_deletesOnlineKey_andStampsLastSeen() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        presenceService.markOffline(42L);

        verify(redisTemplate).delete("presence:42");
        // last-seen stamped as a durable (no-TTL) value.
        verify(valueOps).set(eq("lastseen:42"), any(String.class));
    }

    @Test
    void isOnline_reflectsPresenceKeyExistence() {
        when(redisTemplate.hasKey("presence:42")).thenReturn(true);
        assertThat(presenceService.isOnline(42L)).isTrue();

        when(redisTemplate.hasKey("presence:99")).thenReturn(false);
        assertThat(presenceService.isOnline(99L)).isFalse();
    }

    @Test
    void getPresence_online_reportsOnlineWithNoLastSeen() {
        when(redisTemplate.hasKey("presence:42")).thenReturn(true);

        PresenceResponse presence = presenceService.getPresence(42L);

        assertThat(presence.isOnline()).isTrue();
        assertThat(presence.getLastSeen()).isNull();
        assertThat(presence.getUserId()).isEqualTo(42L);
    }

    @Test
    void getPresence_offline_reportsLastSeenFromRedis() {
        Instant lastSeen = Instant.now().minusSeconds(300);
        when(redisTemplate.hasKey("presence:42")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("lastseen:42")).thenReturn(String.valueOf(lastSeen.toEpochMilli()));

        PresenceResponse presence = presenceService.getPresence(42L);

        assertThat(presence.isOnline()).isFalse();
        assertThat(presence.getLastSeen()).isEqualTo(Instant.ofEpochMilli(lastSeen.toEpochMilli()));
    }
}
