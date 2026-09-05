package com.dawood.nggeen.identity.infrastructure.scheduler;

import com.dawood.nggeen.identity.infrastructure.persistence.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionCleanupService {
    private SessionRepository sessionRepository;

    private final long BATCH_SIZE = 5000;

    @Transactional
    public void executeCleanup(){

    }
}
