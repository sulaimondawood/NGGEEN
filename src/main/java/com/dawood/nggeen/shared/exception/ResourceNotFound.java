package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import org.springframework.http.HttpStatus;

public class ResourceNotFound extends NggeenException {

    public ResourceNotFound(String message) {
        super(message);
    }

    public ResourceNotFound(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }

    public ResourceNotFound(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }
}
