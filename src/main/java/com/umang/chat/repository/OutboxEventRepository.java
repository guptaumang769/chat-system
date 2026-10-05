package com.umang.chat.repository;

import com.umang.chat.model.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /** Oldest unpublished events first — the poller relays these in order. */
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc(Limit limit);
}
