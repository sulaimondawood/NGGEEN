package com.dawood.nggeen.shared.infrastructure.security.service;

import com.dawood.nggeen.account.model.User;
import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.AuthenticationException;
import com.dawood.nggeen.shared.exception.ResourceNotFoundException;
import com.dawood.nggeen.identity.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationContext {
    private final UserRepository userRepository;

    public User getAuthenticatedUser() {
        UserDetails userDetails = getAuthenticatedUserDetails();

        return userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.NOT_FOUND,
                        "User account not found",
                        HttpStatus.NOT_FOUND
                ));
    }

    public UserDetails getAuthenticatedUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException(
                    ErrorCode.UNAUTHORIZED,
                    "User must be authenticated",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails;
        }

        throw new AuthenticationException(
                ErrorCode.UNAUTHORIZED,
                "Invalid authentication principal type",
                HttpStatus.UNAUTHORIZED
        );
    }
}
