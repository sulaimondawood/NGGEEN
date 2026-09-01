package com.dawood.nggeen.identity.application;

import com.dawood.nggeen.account.model.Account;
import com.dawood.nggeen.account.model.EmailVerificationToken;
import com.dawood.nggeen.account.model.User;
import com.dawood.nggeen.account.model.enums.UserStatus;
import com.dawood.nggeen.identity.api.rest.dto.CreateUserRequest;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.ConflictException;
import com.dawood.nggeen.shared.infrastructure.message.amqp.RabbitMQConfig;
import com.dawood.nggeen.account.infrastructure.persistence.AccountRepository;
import com.dawood.nggeen.identity.infrastructure.persistence.UserRepository;
import com.dawood.nggeen.identity.infrastructure.persistence.VerificationTokenRepository;
import com.dawood.nggeen.shared.utils.TokenGeneratorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthApplicationService {
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final VerificationTokenRepository verificationTokenRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createUser(CreateUserRequest request) {
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
                user.getId()
        );
        verificationTokenRepository.save(verificationToken);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NGGEEN_EXCHANGE,
                        RabbitMQConfig.EMAIL_VERIFICATION,
                        Map.of("email", email,
                                "token", rawToken)
                );
                } catch (Exception e) {
                    log.error("CRITICAL_ALERT: Failed to enqueue verification email for user={}. Broker unavailable.", email, e);
                }
            }
        });


    }

}
