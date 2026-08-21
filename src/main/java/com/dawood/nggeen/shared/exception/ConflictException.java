package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import org.springframework.http.HttpStatus;

public class ConflictException extends NggeenException{
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public ConflictException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }
}
