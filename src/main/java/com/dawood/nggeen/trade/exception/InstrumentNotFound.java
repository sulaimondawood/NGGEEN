package com.dawood.nggeen.trade.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import com.dawood.nggeen.shared.exception.NggeenException;
import org.springframework.http.HttpStatus;

public class InstrumentNotFound extends NggeenException {

    public InstrumentNotFound(String message) {
        super(message);
    }

    public InstrumentNotFound(ErrorCode code, String message, HttpStatus status, Throwable cause) {
        super(code, message, status, cause);
    }

    public InstrumentNotFound(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }
}
