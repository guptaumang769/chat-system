package com.umang.chat.repository;

import com.umang.chat.model.entity.Message;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Keyset (seek) pagination: fetch the next page of a conversation's history whose id is
     * greater than the client's cursor, oldest-first. Keyset beats OFFSET here because a
     * chat history grows without bound and OFFSET's cost scales with how deep you page;
     * seeking on the indexed {@code (conversation_id, id)} is O(log n) regardless of depth.
     */
    List<Message> findByConversationIdAndIdGreaterThanOrderByIdAsc(
            Long conversationId, Long afterId, Pageable pageable);

    /** First page (no cursor) — same order, seeded from the start of the conversation. */
    List<Message> findByConversationIdOrderByIdAsc(Long conversationId, Pageable pageable);
}
