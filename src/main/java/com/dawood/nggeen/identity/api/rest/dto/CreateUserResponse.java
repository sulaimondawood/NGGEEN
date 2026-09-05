package com.dawood.nggeen.identity.api.rest.dto;

import com.dawood.nggeen.account.model.enums.UserStatus;

public record CreateUserResponse(
        String email,
        UserStatus status
) {
}
