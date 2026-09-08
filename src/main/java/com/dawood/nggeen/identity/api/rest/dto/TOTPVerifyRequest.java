package com.dawood.nggeen.identity.api.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record TOTPVerifyRequest(
        @NotBlank(message = "Authenticator app code is required") String code,
        @NotBlank(message = "Token is required") String preAuthToken,
        Boolean rememberMe
) {
}
