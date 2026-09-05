package com.dawood.nggeen.identity.service;

import com.dawood.nggeen.account.model.Session;
import com.dawood.nggeen.account.model.User;
import com.dawood.nggeen.identity.infrastructure.persistence.SessionRepository;
import com.dawood.nggeen.shared.utils.HashUtils;
import com.dawood.nggeen.shared.utils.TokenGeneratorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
@Service
public class TokenService {
    private final SessionRepository sessionRepository;

    @Value("${nggeen.security.cookie.secure:true}")
    private boolean secureCookie;

    public String createAndSaveRefreshToken(User existingUser, String clientIp, String userAgent, Instant refreshExpiresAt){

        String refreshToken = TokenGeneratorUtils.generateRandomToken();
        String hashedRefreshToken = HashUtils.hashToken(refreshToken);

        Session session = Session.builder()
                .userId(existingUser.getId())
                .refreshTokenHash(hashedRefreshToken)
                .ip(clientIp)
                .userAgent(userAgent)
                .expiresAt(refreshExpiresAt)
                .build();

        sessionRepository.save(session);

        return refreshToken;

    }

    public ResponseCookie generateRefreshTokenCookie(String rawFreshToken, Duration duration){
        return ResponseCookie.from("refresh_token", rawFreshToken)
                .httpOnly(true)
                .sameSite("Strict")
                .secure(secureCookie)
                .path("/api/v1/auth")
                .maxAge(duration)
                .build();
    }

    public String rotateSessionToken(Session session, String clientIp, String userAgent) {
        String newRefreshToken = TokenGeneratorUtils.generateRandomToken();

        session.setPreviousRefreshTokenHash(session.getRefreshTokenHash());
        session.setRefreshTokenHash(HashUtils.hashToken(newRefreshToken));
        session.setIp(clientIp);
        session.setUserAgent(userAgent);

        sessionRepository.save(session);
        return newRefreshToken;
    }

}
