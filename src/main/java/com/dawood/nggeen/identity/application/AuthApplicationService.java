package com.dawood.nggeen.identity.application;

import com.dawood.nggeen.account.infrastructure.persistence.AccountRepository;
import com.dawood.nggeen.account.model.Account;
import com.dawood.nggeen.account.model.EmailVerificationToken;
import com.dawood.nggeen.account.model.Session;
import com.dawood.nggeen.account.model.User;
import com.dawood.nggeen.account.model.enums.UserStatus;
import com.dawood.nggeen.identity.api.rest.dto.*;
import com.dawood.nggeen.identity.event.UserRegisteredEvent;
import com.dawood.nggeen.identity.infrastructure.persistence.SessionRepository;
import com.dawood.nggeen.identity.infrastructure.persistence.UserRepository;
import com.dawood.nggeen.identity.infrastructure.persistence.VerificationTokenRepository;
import com.dawood.nggeen.identity.infrastructure.security.CloudfareCaptchaValidationService;
import com.dawood.nggeen.identity.service.SessionSecurityService;
import com.dawood.nggeen.identity.service.TokenService;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.AuthenticationException;
import com.dawood.nggeen.shared.exception.BadRequestException;
import com.dawood.nggeen.shared.exception.ConflictException;
import com.dawood.nggeen.shared.exception.ResourceNotFoundException;
import com.dawood.nggeen.shared.infrastructure.message.amqp.RabbitMQConfig;
import com.dawood.nggeen.shared.infrastructure.outbox.model.OutboxEvent;
import com.dawood.nggeen.shared.infrastructure.outbox.model.enums.OutboxEventType;
import com.dawood.nggeen.shared.infrastructure.outbox.persistence.OutboxRepository;
import com.dawood.nggeen.shared.infrastructure.security.jwt.JwtService;
import com.dawood.nggeen.shared.utils.HashUtils;
import com.dawood.nggeen.shared.utils.TokenGeneratorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthApplicationService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final CloudfareCaptchaValidationService cloudfareCaptchaValidationService;
    private final JwtService jwtService;
    private final SessionRepository sessionRepository;
    private final TokenService tokenService;
    private final SessionSecurityService sessionSecurityService;

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request, String clientIp) {
        cloudfareCaptchaValidationService.validateCaptcha(request.getCaptchaToken(), clientIp);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(
                    ErrorCode.EMAIL_EXISTS,
                    "Email already exists",
                    HttpStatus.CONFLICT
            );
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = User.create(email, request.getFullname(), passwordHash, UserStatus.PENDING_VERIFICATION);
        user = userRepository.save(user);

        Account spotAccount = Account.builder()
                .user(user)
                .build();
        accountRepository.save(spotAccount);

        String rawToken = TokenGeneratorUtils.generateRandomToken();
        EmailVerificationToken verificationToken = EmailVerificationToken.create(
                rawToken,
                Instant.now().plus(1, ChronoUnit.DAYS),
                user
        );
        verificationTokenRepository.save(verificationToken);

        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setEmail(email);
        event.setToken(rawToken);
        event.setName(user.getFullName());

        String payload = objectMapper.writeValueAsString(event);
        String exchange = RabbitMQConfig.NGGEEN_EXCHANGE;
        String routingKey = RabbitMQConfig.EMAIL_VERIFICATION;

        OutboxEvent outboxEvent = OutboxEvent.of(OutboxEventType.EMAIL_VERIFICATION, payload, exchange, routingKey);
        outboxRepository.save(outboxEvent);

        return new CreateUserResponse(user.getEmail(), user.getStatus());
    }

    @Transactional
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST,
                    "Invalid or null token ID",
                    HttpStatus.BAD_REQUEST);
        }

        EmailVerificationToken existingToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Token invalid or expired",
                        HttpStatus.NOT_FOUND
                ));

        if (existingToken.getUsedAt() != null) {
            throw new ConflictException(ErrorCode.BAD_REQUEST, "Token has already been used", HttpStatus.BAD_REQUEST);
        }
        if (existingToken.getRevokedAt() != null) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "A newer verification link was already requested", HttpStatus.BAD_REQUEST);
        }
        if (Instant.now().isAfter(existingToken.getExpiresAt())) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Verification token has expired", HttpStatus.BAD_REQUEST);
        }

        User user = existingToken.getUser();
        user.setStatus(UserStatus.ACTIVE);

        existingToken.setUsedAt(Instant.now());

        userRepository.save(user);
        verificationTokenRepository.save(existingToken);
    }

    @Transactional
    public LoginResult login(LoginRequest payload, String clientIp, String userAgent) {
        cloudfareCaptchaValidationService.validateCaptcha(payload.captchaToken(), clientIp);

        String email = payload.email().trim().toLowerCase();
        User existingUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.BAD_REQUEST,
                        "Invalid email or password",
                        HttpStatus.BAD_REQUEST
                ));

        if (!passwordEncoder.matches(payload.password(), existingUser.getPasswordHash())) {
            throw new BadRequestException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid email or password",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!existingUser.canTrade()) {
            throw new BadRequestException(
                    ErrorCode.BAD_REQUEST,
                    "Verify your account. Check your inbox/spam to verify your email address.",
                    HttpStatus.BAD_REQUEST
            );
        }


        String accessToken = createToken(existingUser);

        Duration refreshDuration = payload.rememberMe() ? Duration.ofDays(30) : Duration.ofDays(1);
        Instant refreshExpiresAt = Instant.now().plus(refreshDuration);

        String refreshToken = tokenService.createAndSaveRefreshToken(existingUser, clientIp, userAgent, refreshExpiresAt);

        LoginResponse loginResponse = new LoginResponse(
                accessToken,
                new UserDTO(existingUser.getId(),
                        existingUser.getEmail(),
                        existingUser.getFullName(),
                        existingUser.getRole()));

        return new LoginResult(loginResponse, refreshToken, refreshDuration);
    }

    @Transactional
    public RefreshResult refreshAccessToken(String rawRefreshToken, String clientIp, String userAgent) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthenticationException(ErrorCode.FORBIDDEN, "Missing refresh token cookie", HttpStatus.UNAUTHORIZED);
        }

        String hashedToken = HashUtils.hashToken(rawRefreshToken);
        Optional<Session> currentMatch = sessionRepository.findByRefreshTokenHash(hashedToken);

        if (currentMatch.isPresent()) {
            Session session = currentMatch.get();
            if (!session.isActive()) {
                throw new BadRequestException(
                        ErrorCode.UNAUTHORIZED,
                        "Refresh token has expired. Please log in again.",
                        HttpStatus.UNAUTHORIZED
                );
            }

            User user = userRepository.findById(session.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.BAD_REQUEST,
                            "User account not found",
                            HttpStatus.NOT_FOUND
                    ));

            String newRefreshToken = tokenService.rotateSessionToken(session, clientIp, userAgent);

            Duration remainingDuration = Duration.between(Instant.now(), session.getExpiresAt());
            if (remainingDuration.isNegative()) {
                remainingDuration = Duration.ZERO;
            }

            ResponseCookie cookie = tokenService.generateRefreshTokenCookie(newRefreshToken, remainingDuration);
            String accessToken = createToken(user);

            return new RefreshResult(cookie, accessToken);

        }

        Optional<Session> previousMatch = sessionRepository.findByPreviousRefreshTokenHash(hashedToken);
        if (previousMatch.isPresent()) {
            Session session = previousMatch.get();

            if (session.isRevoked()) {
                throw new AuthenticationException(
                        ErrorCode.UNAUTHORIZED,
                        "Session is invalid. Please log in again.",
                        HttpStatus.UNAUTHORIZED
                );
            }
            log.error("SECURITY ALERT: Refresh token reuse detected for user {}!", session.getUserId());

            sessionSecurityService.executeBreachKillSwitch(
                    session.getUserId(),
                    Session.RevokeReason.REUSE_DETECTED
            );

            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED,
                    "Suspicious activity detected. All sessions terminated.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        throw new AuthenticationException(
                ErrorCode.UNAUTHORIZED,
                "Invalid refresh token.",
                HttpStatus.UNAUTHORIZED);

    }

    private String createToken(User existingUser) {
        Map<String, Object> claims = Map.of(
                "userId", existingUser.getId().toString(),
                "role", existingUser.getRole().name()
        );

        return jwtService.createToken(claims, existingUser.getEmail());
    }
}
