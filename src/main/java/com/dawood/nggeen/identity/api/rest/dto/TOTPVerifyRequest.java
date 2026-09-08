package com.dawood.nggeen.identity.api.rest.dto;

public record TOTPVerifyRequest(
        String code,
        String preAuthToken,
        Boolean rememberMe
) {
}
