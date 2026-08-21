package com.dawood.nggeen.account.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.NggeenException;
import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends NggeenException {
    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public InsufficientBalanceException(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }
}
