package com.dawood.nggeen.identity.infrastructure.scheduler;

import com.dawood.nggeen.identity.infrastructure.persistence.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupService {
    private final SessionRepository sessionRepository;

    @Value("${nggeen.security.session.cleanup.retention-days:30}")
    private int retentionDays;

    @Value("${nggeen.security.session.cleanup.batch-size:1000}")
    private int batchSize;

    @Transactional
    public int executeCleanup() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Starting session cleanup. Retention: {} days. Cutoff timestamp: {}", retentionDays, cutoff);

        int executedCount = sessionRepository.deleteStaleFamilySessions(cutoff, batchSize);
        log.info("Session cleanup completed for stale session records.");

        return executedCount;
    }
}
