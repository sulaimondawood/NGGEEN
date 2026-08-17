package com.dawood.nggeen.trade.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.NggeenException;
import org.springframework.http.HttpStatus;

public class InvalidOrderRequestException extends NggeenException {
    public InvalidOrderRequestException(String message) {
        super(message);
    }

    public InvalidOrderRequestException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public InvalidOrderRequestException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }
}
