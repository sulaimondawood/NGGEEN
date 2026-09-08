package com.dawood.nggeen.identity.api.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record Disable2faRequest(
        @NotBlank(message = "Password is required") String currentPassword,
        @NotBlank(message = "Code is required") String code
) {
}
