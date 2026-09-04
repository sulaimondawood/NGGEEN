package com.dawood.nggeen.identity.infrastructure.security.dto;

public record CaptchaResponse(
        boolean success,
        String[] errorCodes,
        String challenge_ts,
        String hostname
) {
}
