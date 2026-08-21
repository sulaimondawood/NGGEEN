package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import org.springframework.http.HttpStatus;

public class AuthenticationException extends NggeenException{
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public AuthenticationException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }
}
