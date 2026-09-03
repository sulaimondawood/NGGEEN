package com.dawood.nggeen.identity.infrastructure.message;

import com.dawood.nggeen.identity.event.UserRegisteredEvent;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.InfrastructureException;
import com.dawood.nggeen.shared.infrastructure.message.amqp.RabbitMQConfig;
import com.dawood.nggeen.shared.infrastructure.message.mail.EmailService;
import com.dawood.nggeen.shared.infrastructure.outbox.model.enums.OutboxEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationConsumer {
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;

    @Value("${nggeen.client.url}")
    private String clientUrl;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_VERIFICATION_QUEUE)
    public void consumeEmailMessage(String payload, MessageProperties messageProperties) {
        String eventType = messageProperties.getHeader("eventType");
        log.debug("Received event type: {} for messageId: {}", eventType, messageProperties.getMessageId());

        if (!OutboxEventType.EMAIL_VERIFICATION.name().equals(eventType)) {
            log.warn("Discarding unexpected eventType [{}] on queue [{}], messageId: {}",
                    eventType, RabbitMQConfig.EMAIL_VERIFICATION_QUEUE, messageProperties.getMessageId());
            return;
        }

        try {
            UserRegisteredEvent message = objectMapper.readValue(payload, UserRegisteredEvent.class);

            String token = message.getToken();
            String name = message.getName();
            String email = message.getEmail();
            String expiresIn = "24 hrs";
            String verificationLink = String.format(clientUrl + "auth/verify?token=%s", token);

            Context ctx = new Context();
            ctx.setVariable("username", name);
            ctx.setVariable("expiryMinutes", expiresIn);
            ctx.setVariable("verificationLink", verificationLink);

            String body = templateEngine.process("auth/onboarding-verification-email", ctx);
            emailService.sendRichEmail(email, body, "Verify your email address | Nggeen");
            log.info("Verification email successfully dispatched to: {}", email);

        } catch (Exception e) {
            log.error("Failed to process email verification message for messageId: {}",
                    messageProperties.getMessageId(), e);
            throw new InfrastructureException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Error consuming email message",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }
}
