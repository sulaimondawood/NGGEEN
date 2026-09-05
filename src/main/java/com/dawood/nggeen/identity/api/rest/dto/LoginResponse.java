package com.dawood.nggeen.identity.api.rest.dto;

public record LoginResponse(
        String accessToken,
        UserDTO user
) {
}
