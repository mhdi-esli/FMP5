package com.bank.messaging.exception;

import com.bank.messaging.dto.MessageResponse;
import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.enums.ErrorCode;
import com.bank.messaging.enums.MessageStatus;
import com.bank.messaging.enums.ValidationResultEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle bean validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ValidationError(
                ErrorCode.MSG_001.getCode(),
                error.getField(),
                error.getDefaultMessage()
            ))
            .collect(Collectors.toList());

        log.warn("Validation errors: {}", errors);

        MessageResponse response = new MessageResponse(
            null,
            null,
            null,
            MessageStatus.VALIDATION_FAILED,
            LocalDateTime.now(),
            ValidationResultEnum.FAILED,
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle enum conversion errors (e.g., invalid network value).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<MessageResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String fieldName = ex.getName();
        String invalidValue = ex.getValue() != null ? ex.getValue().toString() : "null";

        log.warn("Invalid value '{}' for field '{}'", invalidValue, fieldName);

        ValidationError error;
        if ("network".equals(fieldName)) {
            error = new ValidationError(
                ErrorCode.MSG_003.getCode(),
                fieldName,
                ErrorCode.MSG_003.getPersianMessage()
            );
        } else {
            error = new ValidationError(
                ErrorCode.MSG_001.getCode(),
                fieldName,
                "مقدار نامعتبر است"
            );
        }

        MessageResponse response = new MessageResponse(
            null,
            null,
            null,
            MessageStatus.VALIDATION_FAILED,
            LocalDateTime.now(),
            ValidationResultEnum.FAILED,
            List.of(error)
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGenericError(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ValidationError error = new ValidationError(
            ErrorCode.MSG_007.getCode(),
            null,
            ErrorCode.MSG_007.getPersianMessage()
        );

        MessageResponse response = new MessageResponse(
            null,
            null,
            null,
            MessageStatus.VALIDATION_FAILED,
            LocalDateTime.now(),
            ValidationResultEnum.FAILED,
            List.of(error)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
