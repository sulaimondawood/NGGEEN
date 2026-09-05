package com.dawood.nggeen.shared.infrastructure.outbox.persistence;

import com.dawood.nggeen.shared.infrastructure.outbox.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
                AND next_retry_at <= CAST(:currentTime AS TIMESTAMP WITH TIME ZONE)
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findPendingBatchEventsForProcessing(
            @Param("currentTime") Instant currentTime,
            @Param("limit") int limit);
}
