package com.dawood.nggeen.account.infrastructure.message.mail;

import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.InfrastructureException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConfig {
    private final JavaMailSender mailSender;

    @Value("${nggeen.message.mail-sub}")
    private String from;


    private static final String SENDER_NAME = "Nggeen";

    public void sendRichEmail(String to, String body, String subject) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.displayName()
            );

            helper.setFrom(from, SENDER_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(mimeMessage);
            log.info("Email successfully dispatched to {}", to);

        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new InfrastructureException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to deliver email message",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
