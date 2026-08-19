package com.dawood.nggeen.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        ErrorCode code,
        String message,
        String path,
        Map<String, String> validations,
        Instant timestamp
) {

    public static ApiError of(int status, ErrorCode code, String message, String path) {
        return new ApiError(status, code, message, path, null, Instant.now());
    }

    public static ApiError ofValidation(int status, ErrorCode code, String message, Map<String, String> validations, String path) {
        return new ApiError(status, code, message, path, validations, Instant.now());
    }
}
