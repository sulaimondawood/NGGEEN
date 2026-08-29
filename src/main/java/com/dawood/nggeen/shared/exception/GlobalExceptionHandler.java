package com.dawood.nggeen.shared.exception;

import com.dawood.nggeen.shared.dto.ApiError;
import com.dawood.nggeen.shared.dto.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest req) {
        int status = HttpStatus.BAD_REQUEST.value();

        Map<String, String> errors = new HashMap<>();
        ex.getFieldErrors().forEach((e) -> {
            errors.put(e.getField(), e.getDefaultMessage());
        });

        ApiError error = ApiError.ofValidation(
                status,
                ErrorCode.BAD_REQUEST,
                "Validation failed for request parameters",
                errors,
                req.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(NggeenException.class)
    public ResponseEntity<ApiError> handleBusinessException(NggeenException ex, HttpServletRequest req) {
        ApiError error = ApiError.of(
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatus().value()).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleBusinessException(NoResourceFoundException ex, HttpServletRequest req) {
        int status = HttpStatus.NOT_FOUND.value();
        ApiError error = ApiError.of(
                status,
                ErrorCode.RESOURCE_NOT_FOUND,
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        ApiError error = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Something went wrong",
                request.getRequestURI()
        );

        return ResponseEntity.internalServerError().body(error);
    }
}
