package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import org.springframework.http.HttpStatus;

public class BadRequestException extends NggeenException{
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public BadRequestException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }
}
