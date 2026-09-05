package com.dawood.nggeen.identity.infrastructure.security;

import com.dawood.nggeen.identity.infrastructure.security.contract.CaptchaValidation;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudfareCaptchaValidationService implements CaptchaValidation {

    private final RestClient restClient = RestClient.builder().build();

    @Value("${nggeen.security.cloud-fare.turnstile.secret-key}")
    private String CLOUDFARE_TURNSTILE_SECRET;

    private final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Override
    public void validateCaptcha(String token, String clientIp) {
        Map<String, String> params = new HashMap<>();
        params.put("secret", CLOUDFARE_TURNSTILE_SECRET);
        params.put("response", token);

        if (clientIp != null) {
            params.put("remoteip", clientIp);
        }

        try {
            CaptchaResponse response = restClient.post()
                    .uri(TURNSTILE_VERIFY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(params)
                    .retrieve()
                    .body(CaptchaResponse.class);

            if (response == null || !response.success()) {
                log.warn("CAPTCHA validation failed. Error codes: {}",
                        response != null ? response.errorCodes() : "null response");
                throw new BadRequestException(
                        ErrorCode.BAD_REQUEST,
                        "Captcha verification failed. Please try again.",
                        HttpStatus.BAD_REQUEST);
            }

        } catch (RestClientException e) {
            log.error("Error communicating with Cloudflare Turnstile API", e);
            throw new BadRequestException(
                    ErrorCode.BAD_REQUEST,
                    "Unable to complete bot verification. Please try again later.",
                    HttpStatus.BAD_REQUEST);
        }

    }

    public record CaptchaResponse(
            boolean success,
            String[] errorCodes,
            String challenge_ts,
            String hostname
    ) {
    }
}
