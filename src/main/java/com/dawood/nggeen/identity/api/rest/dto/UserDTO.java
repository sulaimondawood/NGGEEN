package com.dawood.nggeen.identity.api.rest.dto;

import com.dawood.nggeen.account.model.enums.UserRole;

import java.util.UUID;

public record UserDTO(
        UUID id,
        String email,
        String fullName,
        UserRole role
) {
}
