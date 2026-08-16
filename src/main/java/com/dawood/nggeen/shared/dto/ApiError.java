package com.dawood.nggeen.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        HttpStatus status,
        ErrorCode code,
        String message,
        String path,
        Map<String, String> validations,
        Instant timestamp
) {

    public static ApiError of(HttpStatus status, ErrorCode code, String message, String path) {
        return new ApiError(status, code, message, path, null, Instant.now());
    }

    public static ApiError ofValidation(HttpStatus status, ErrorCode code, String message, Map<String, String> validations, String path) {
        return new ApiError(status, code, message, path, validations, Instant.now());
    }
}
