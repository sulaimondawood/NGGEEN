package com.dawood.nggeen.account.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens",
        indexes = {@Index(name = "idx_email_verification_token", columnList = "token")}
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EmailVerificationToken {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private Instant usedAt;

    public static EmailVerificationToken create(String token, Instant expiresAt, User user) {
        return EmailVerificationToken
                .builder()
                .token(token)
                .expiresAt(expiresAt)
                .user(user)
                .build();
    }

    public boolean isValid() {
        return usedAt == null && revokedAt == null && Instant.now().isBefore(expiresAt);
    }
}
