package com.umang.chat.ws;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Tracks which users have a live WebSocket on this node. */
@Component
public class LocalSessionRegistry {

    // userId -> node id string is not needed here; presence in this set == connected locally.
    private final Set<Long> localUsers = ConcurrentHashMap.newKeySet();

    public void add(Long userId) {
        localUsers.add(userId);
    }

    public void remove(Long userId) {
        localUsers.remove(userId);
    }

    public boolean isLocal(Long userId) {
        return localUsers.contains(userId);
    }

    public Set<Long> getLocalUsers() {
        return Set.copyOf(localUsers);
    }
}
