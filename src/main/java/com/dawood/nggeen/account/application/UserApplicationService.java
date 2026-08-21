package com.dawood.nggeen.account.application;

import com.dawood.nggeen.account.api.rest.dto.CreateUserRequest;
import com.dawood.nggeen.account.infrastructure.message.amqp.RabbitMQConfig;
import com.dawood.nggeen.account.infrastructure.persistence.UserRepository;
import com.dawood.nggeen.account.infrastructure.persistence.VerificationTokenRepository;
import com.dawood.nggeen.account.model.EmailVerificationToken;
import com.dawood.nggeen.account.model.User;
import com.dawood.nggeen.account.model.enums.UserStatus;
import com.dawood.nggeen.shared.TokenGeneratorUtils;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserApplicationService {
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final VerificationTokenRepository verificationTokenRepository;

    @Transactional
    public void createUser(CreateUserRequest request) {
        String cleanedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(cleanedEmail)) {
            throw new ConflictException(
                    ErrorCode.EMAIL_EXISTS,
                    "Email already exists",
                    HttpStatus.CONFLICT
            );
        }

        User newUser = User.create(
                request.getEmail(),
                request.getUsername(),
                request.getPassword(),
                UserStatus.ACTIVE
        );

        User savedUser = userRepository.save(newUser);

        TokenGeneratorUtils tokenGenerator = new TokenGeneratorUtils();
        EmailVerificationToken verificationToken = EmailVerificationToken.create(
                tokenGenerator.generateRandomToken(),
                Instant.now().plus(1, ChronoUnit.DAYS),
                savedUser.getId()
        );

        EmailVerificationToken savedVerificationToken = verificationTokenRepository.save(verificationToken);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Map<String, String> event = new HashMap<>();
                event.put("email", cleanedEmail);
                event.put("token", savedVerificationToken.getTokenHash());

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NGGEEN_EXCHANGE,
                        RabbitMQConfig.EMAIL_VERIFICATION,
                        event
                        );
            }
        });


    }

}
