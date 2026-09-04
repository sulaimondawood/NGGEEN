package com.dawood.nggeen.identity.api.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "A valid email address is required")
    private String email;

    @NotBlank(message = "Username is required")
    private String fullname;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Captcha Token is required")
    private String captchaToken;
}
