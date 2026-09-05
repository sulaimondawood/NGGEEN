package com.dawood.nggeen.identity.infrastructure.persistence;

import com.dawood.nggeen.account.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByRefreshTokenHash(String tokenHash);

    Optional<Session> findByPreviousRefreshTokenHash(String previousTokenHash);

    @Modifying
    @Query("""
                UPDATE Session s
                SET s.revoked = true,
                    s.revokedAt = :now,
                    s.revokeReason = :reason
                WHERE s.userId = :userId AND s.revoked = false
            """)
    void revokeAllActiveSessionsForUser(
            @Param("userId") UUID userId,
            @Param("now") Instant now,
            @Param("reason") Session.RevokeReason reason
    );

}
