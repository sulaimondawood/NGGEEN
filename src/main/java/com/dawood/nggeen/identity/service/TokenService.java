package com.dawood.nggeen.identity.service;

import com.dawood.nggeen.account.model.Session;
import com.dawood.nggeen.account.model.User;
import com.dawood.nggeen.identity.infrastructure.persistence.SessionRepository;
import com.dawood.nggeen.shared.utils.HashUtils;
import com.dawood.nggeen.shared.utils.TokenGeneratorUtils;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class TokenService {
    private final SessionRepository sessionRepository;

    @Value("${nggeen.security.cookie.secure:true}")
    private boolean secureCookie;

    public String createAndSaveRefreshToken(User existingUser, String clientIp, String userAgent, Instant ttl) {

        UUID familyId = UuidCreator.getTimeOrderedEpoch();
        String refreshToken = TokenGeneratorUtils.generateRandomToken();

        Session session = Session.builder()
                .userId(existingUser.getId())
                .familyId(familyId)
                .refreshTokenHash(HashUtils.hashToken(refreshToken))
                .expiresAt(ttl)
                .status(Session.Status.ACTIVE)
                .ip(clientIp != null ? clientIp : "unknown")
                .userAgent(userAgent)
                .build();

        sessionRepository.save(session);
        return refreshToken;
    }

    public ResponseCookie generateRefreshTokenCookie(String rawFreshToken, Duration duration) {
        return ResponseCookie.from("refresh_token", rawFreshToken)
                .httpOnly(true)
                .sameSite("Strict")
                .secure(secureCookie)
                .path("/api/v1/auth")
                .maxAge(duration)
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .sameSite("Strict")
                .secure(secureCookie)
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

}
