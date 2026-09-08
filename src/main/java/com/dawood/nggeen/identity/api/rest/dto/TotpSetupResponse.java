package com.dawood.nggeen.identity.api.rest.dto;

public record TotpSetupResponse(
        String secret,
        String otpAuthUri
) {
}
