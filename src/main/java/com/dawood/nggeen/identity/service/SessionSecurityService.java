package com.dawood.nggeen.identity.service;

import com.dawood.nggeen.account.model.Session;
import com.dawood.nggeen.identity.infrastructure.persistence.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionSecurityService {
    private final SessionRepository sessionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeBreachKillSwitch(UUID userId) {
        sessionRepository.revokeAllActiveSessionsForUser(userId, Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeRevokeFamilyKillSwitch(UUID familyId) {
        sessionRepository.revokeFamily(familyId, Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Session session) {
        sessionRepository.save(session);
    }
}
