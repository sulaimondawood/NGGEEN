package com.dawood.nggeen.identity.infrastructure.persistence;

import com.dawood.nggeen.account.model.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Session> findByRefreshTokenHash(String tokenHash);

    @Modifying
    @Query("""
                UPDATE Session s
                SET s.status = 'REVOKED',
                    s.revokedAt = :now
                WHERE s.userId = :userId AND s.status <> 'REVOKED'
            """)
    void revokeAllActiveSessionsForUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now
    );

    @Query("""
                UPDATE Session s
                SET s.status = 'REVOKED',
                    s.revokedAt = :now
                WHERE s.family_id= :familyId AND s.status <> 'REVOKED'
            """)
    void revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

}
