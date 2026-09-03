package com.dawood.nggeen.shared.infrastructure.outbox.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxPollerWorker {
    private final OutboxEventProcessor outboxEventProcessor;

    @Scheduled(fixedDelayString = "${nggeen.outbox.poller.fixed-delay:2000}")
    public void poll() {
        outboxEventProcessor.processBatch();
    }
}
