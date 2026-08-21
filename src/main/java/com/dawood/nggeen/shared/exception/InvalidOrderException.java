package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidOrderException extends NggeenException {
    public InvalidOrderException(String message) {
        super(message);
    }

    public InvalidOrderException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public InvalidOrderException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }
}
