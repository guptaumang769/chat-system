package com.umang.chat.ws;

import com.umang.chat.service.PresenceService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/** Hooks STOMP CONNECT/DISCONNECT to flip presence and update the local session registry. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceChannelInterceptor implements ChannelInterceptor {

    private final PresenceService presenceService;
    private final LocalSessionRegistry localSessions;

    @Value("${chat.node-id:node-1}")
    private String nodeId;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(command)) {
            Long userId = parseUserId(accessor.getFirstNativeHeader("userId"));
            if (userId != null) {
                accessor.setUser(new UserPrincipal(userId));
                presenceService.markOnline(userId, nodeId);
                localSessions.add(userId);
                log.debug("User {} connected on {}", userId, nodeId);
            }
        } else if (StompCommand.DISCONNECT.equals(command)) {
            Long userId = principalUserId(accessor.getUser());
            if (userId != null) {
                presenceService.markOffline(userId);
                localSessions.remove(userId);
                log.debug("User {} disconnected from {}", userId, nodeId);
            }
        }
        return message;
    }

    private Long parseUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long principalUserId(Principal principal) {
        return principal instanceof UserPrincipal up ? up.userId() : null;
    }

    public record UserPrincipal(Long userId) implements Principal {
        @Override
        public String getName() {
            return String.valueOf(userId);
        }
    }
}
