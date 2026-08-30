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
                @Index(name = "idx_sessions_token_hash", columnList = "refresh_token_hash", unique = true)
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

    private String ip;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    private RevokeReason revokeReason;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private Instant lastUsedAt;

    public static enum RevokeReason {
        USER_LOGOUT,
        REUSE_DETECTED,
        EXPIRED,
        PASSWORD_RESET
    }
}
