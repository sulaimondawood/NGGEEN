package com.dawood.nggeen.identity.infrastructure.scheduler;

import com.dawood.nggeen.identity.infrastructure.persistence.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionCleanupScheduler {
    private SessionRepository sessionRepository;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup(){

    }
}
