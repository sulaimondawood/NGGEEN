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
                @Index(name = "idx_sessions_family_id_status", columnList = "family_id, status"),
                @Index(name = "idx_sessions_user_id_status", columnList = "user_id, status")
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

    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false, unique = true)
    private String refreshTokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    private Instant revokedAt;

    @Column(nullable = false)
    private String ip;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    public  enum Status {
        ACTIVE,
        USED,
        REVOKED
    }

    public boolean isActive() {
        return status == Status.ACTIVE && Instant.now().isBefore(expiresAt);
    }

}
