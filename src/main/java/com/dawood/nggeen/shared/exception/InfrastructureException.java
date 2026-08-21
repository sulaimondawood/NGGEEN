package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import org.springframework.http.HttpStatus;

public class InfrastructureException extends NggeenException {
    public InfrastructureException(String message) {
        super(message);
    }

    public InfrastructureException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public InfrastructureException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }
}
