package com.dawood.nggeen.identity.api.rest.dto;

import java.time.Duration;

public record LoginResult(
        LoginResponse loginResponse,
        String refreshToken,
        Duration refreshDuration
) {

}
