package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends NggeenException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }

    public ResourceNotFoundException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }
}
