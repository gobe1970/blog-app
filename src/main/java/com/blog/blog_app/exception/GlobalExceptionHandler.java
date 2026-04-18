package com.blog.blog_app.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice   // Intercepts exceptions from ALL @Controller / @RestController classes
@Slf4j
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ //
    //  404 — Resource Not Found                                            //
    // ------------------------------------------------------------------ //
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found: {} — path='{}'", ex.getMessage(), request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())           // 404
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())  // "Not Found"
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ------------------------------------------------------------------ //
    //  400 — Bad Request                                                   //
    // ------------------------------------------------------------------ //
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        log.warn("Bad request: {} — path='{}'", ex.getMessage(), request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ------------------------------------------------------------------ //
    //  422 — Validation Failures (@Valid on request body/model attr)      //
    // ------------------------------------------------------------------ //
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // Collect every field-level error into a map: { "title": "Title is required" }
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String field   = ((FieldError) error).getField();
                    String message = error.getDefaultMessage();
                    fieldErrors.put(field, message);
                });

        log.warn("Validation failed on path='{}': {}", request.getRequestURI(), fieldErrors);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())            // 422
                .error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())   // "Unprocessable Entity"
                .message("Validation failed. See 'validationErrors' for details.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .validationErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    // ------------------------------------------------------------------ //
    //  413 — File Too Large                                                //
    // ------------------------------------------------------------------ //
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        log.warn("File upload too large — path='{}'", request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())           // 413
                .error(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase())
                .message("File size exceeds the maximum allowed limit.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    // ------------------------------------------------------------------ //
    //  500 — Catch-All for Unexpected Exceptions                          //
    // ------------------------------------------------------------------ //
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        // Log full stack trace for unexpected errors — this is the one place it belongs
        log.error("Unexpected error on path='{}': {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())           // 500
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}