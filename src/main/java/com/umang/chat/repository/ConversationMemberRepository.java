package com.umang.chat.repository;

import com.umang.chat.model.entity.ConversationMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    List<ConversationMember> findByConversationId(Long conversationId);

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    /** Just the user ids of a conversation's members — the fan-out target set. */
    @Query("select m.userId from ConversationMember m where m.conversationId = :conversationId")
    List<Long> findMemberUserIds(Long conversationId);
}
