package com.dawood.nggeen.account.model;

import com.dawood.nggeen.shared.model.MetaData;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Table(name = "sessions",
        indexes = {
                @Index(name = "idx_session_user_id", columnList = "user_id"),
                @Index(name = "idx_sessions_token_hash", columnList = "refresh_token_hash", unique = true),
                @Index(name = "idx_sessions_prev_token_hash", columnList = "previous_refresh_token_hash")
        }
)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Session extends MetaData {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String refreshTokenHash;

    private String previousRefreshTokenHash;

    @Column(nullable = false)
    private String ip;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    private RevokeReason revokeReason;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    private Instant revokedAt;

    public static enum RevokeReason {
        USER_LOGOUT,
        REUSE_DETECTED,
        EXPIRED,
        PASSWORD_RESET
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

}
