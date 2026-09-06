package com.dawood.nggeen.identity.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionCleanupScheduler {
    private final SessionCleanupService sessionCleanupService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup(){
        int deleted;
        do {
            deleted = sessionCleanupService.executeCleanup();
        } while (deleted > 0);
    }
}
