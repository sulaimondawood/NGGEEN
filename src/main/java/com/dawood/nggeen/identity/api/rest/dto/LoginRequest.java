package com.dawood.nggeen.identity.api.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
       @NotBlank(message = "Email address is required") String email,
       @NotBlank(message = "Password is required") String password,
       @NotBlank(message = "Captcha token is required") String captchaToken,
       Boolean rememberMe
) {
}
