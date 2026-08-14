package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NggeenException extends RuntimeException {

    private HttpStatus status;
    private ErrorCode code;

    public NggeenException(String message) {
        super(message);
    }

    public NggeenException(ErrorCode code, String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public NggeenException(ErrorCode code, String message, HttpStatus status,Throwable cause ) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
