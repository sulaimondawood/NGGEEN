package com.dawood.nggeen.shared.infrastructure.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.AuthenticationException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class JwtService {

    private static final String ISSUER = "nggeen";
    private static final Duration DEFAULT_EXPIRY = Duration.ofMinutes(5);

    @Value("${nggeen.security.secret-key}")
    private String secretKey;

    private JWTVerifier verifier;
    private Algorithm algorithm;

    @PostConstruct
    public void init() {
        algorithm = Algorithm.HMAC256(secretKey);
        verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
    }

    public String createToken(Map<String, String> claims, String subject) {
        try {
            var builder = JWT.create()
                    .withJWTId(UUID.randomUUID().toString())
                    .withIssuer(ISSUER)
                    .withSubject(subject)
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(Instant.now().plus(DEFAULT_EXPIRY));

            if (claims != null && !claims.isEmpty()) {
                claims.forEach(builder::withClaim);
            }

            return builder.sign(algorithm);

        } catch (JWTCreationException e) {
            log.error("Failed to sign JWT token for subject: {}", subject, e);
            throw new AuthenticationException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error generating authentication token",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public DecodedJWT verifyAndDecodeToken(String token) {
        try {
            return verifier.verify(token);

        } catch (JWTVerificationException e) {
            log.warn("Invalid or expired JWT token: {}", e.getMessage());
            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED,
                    "Invalid or expired authentication token",
                    HttpStatus.UNAUTHORIZED
            );
        }
    }
}
