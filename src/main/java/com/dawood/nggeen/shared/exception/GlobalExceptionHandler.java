package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ApiError;
import com.dawood.nggeen.shared.dto.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NggeenException.class)
    public ResponseEntity<ApiError> handleBusinessException(NggeenException ex, HttpServletRequest req) {
        ApiError error = ApiError.of(
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus().value()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        ApiError error = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Something went wrong",
                request.getRequestURI()
        );

        return ResponseEntity.internalServerError().body(error);
    }
}
