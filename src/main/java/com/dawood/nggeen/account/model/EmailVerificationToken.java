package com.dawood.nggeen.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
@Builder
@AllArgsConstructor
@Getter
public class EmailVerificationToken {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false, unique = true)
    private Instant expiresAt;

    private Instant usedAt;

    public static EmailVerificationToken create(String token, Instant expiresAt, UUID userId) {
        return EmailVerificationToken
                .builder()
                .tokenHash(token)
                .expiresAt(expiresAt)
                .userId(userId)
                .build();
    }

    public boolean isValid() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }
}
