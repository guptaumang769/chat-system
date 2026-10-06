package com.umang.chat.service;

import com.umang.chat.dto.response.PresenceResponse;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** Redis-backed online/offline presence with TTL auto-expiry and durable last-seen. */
@Service
@RequiredArgsConstructor
public class PresenceService {

    static final String ONLINE_PREFIX = "presence:";
    static final String LAST_SEEN_PREFIX = "lastseen:";

    static final Duration ONLINE_TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, String> redisTemplate;

    public void markOnline(Long userId, String nodeId) {
        redisTemplate.opsForValue().set(onlineKey(userId), nodeId, ONLINE_TTL);
    }

    public void markOffline(Long userId) {
        redisTemplate.delete(onlineKey(userId));
        redisTemplate.opsForValue().set(lastSeenKey(userId), String.valueOf(Instant.now().toEpochMilli()));
    }

    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey(userId)));
    }

    public PresenceResponse getPresence(Long userId) {
        boolean online = isOnline(userId);
        Instant lastSeen = null;
        if (!online) {
            String raw = redisTemplate.opsForValue().get(lastSeenKey(userId));
            if (raw != null) {
                lastSeen = Instant.ofEpochMilli(Long.parseLong(raw));
            }
        }
        return PresenceResponse.builder()
                .userId(userId)
                .online(online)
                .lastSeen(lastSeen)
                .build();
    }

    private String onlineKey(Long userId) {
        return ONLINE_PREFIX + userId;
    }

    private String lastSeenKey(Long userId) {
        return LAST_SEEN_PREFIX + userId;
    }
}
