package com.dawood.nggeen.identity.application;

import com.dawood.nggeen.account.infrastructure.persistence.AccountRepository;
import com.dawood.nggeen.account.model.Account;
import com.dawood.nggeen.account.model.EmailVerificationToken;
import com.dawood.nggeen.account.model.User;
import com.dawood.nggeen.account.model.enums.UserStatus;
import com.dawood.nggeen.identity.api.rest.dto.CreateUserRequest;
import com.dawood.nggeen.identity.api.rest.dto.CreateUserResponse;
import com.dawood.nggeen.identity.event.UserRegisteredEvent;
import com.dawood.nggeen.identity.infrastructure.persistence.UserRepository;
import com.dawood.nggeen.identity.infrastructure.persistence.VerificationTokenRepository;
import com.dawood.nggeen.identity.infrastructure.persistence.projection.TokenWithUserView;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.BadRequestException;
import com.dawood.nggeen.shared.exception.ConflictException;
import com.dawood.nggeen.shared.exception.ResourceNotFoundException;
import com.dawood.nggeen.shared.infrastructure.message.amqp.RabbitMQConfig;
import com.dawood.nggeen.shared.infrastructure.outbox.model.OutboxEvent;
import com.dawood.nggeen.shared.infrastructure.outbox.model.enums.OutboxEventType;
import com.dawood.nggeen.shared.infrastructure.outbox.persistence.OutboxRepository;
import com.dawood.nggeen.shared.utils.TokenGeneratorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
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
        event.setName(user.getFullname());

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
            throw new ConflictException(ErrorCode.BAD_REQUEST, "Token has already been used",HttpStatus.BAD_REQUEST);
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
}
