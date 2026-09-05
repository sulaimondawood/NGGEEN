package com.dawood.nggeen.identity.api.rest.dto;

import org.springframework.http.ResponseCookie;

public record RefreshResult(
        ResponseCookie cookie,
        String accessToken
) {
}
