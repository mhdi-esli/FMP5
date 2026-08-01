package com.bank.messaging.exception;

import com.bank.messaging.dto.DefinitionErrorResponse;
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
            .toList();

        log.warn("Validation errors: {}", errors);

        return ResponseEntity.badRequest().body(failureResponse(errors));
    }

    /**
     * Handle enum conversion errors (e.g., invalid network value).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<MessageResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String fieldName = ex.getName();
        log.warn("Invalid value '{}' for field '{}'", ex.getValue(), fieldName);

        ErrorCode code = "network".equals(fieldName) ? ErrorCode.MSG_003 : ErrorCode.MSG_001;
        ValidationError error = new ValidationError(code.getCode(), fieldName, code.getPersianMessage());

        return ResponseEntity.badRequest().body(failureResponse(List.of(error)));
    }

    /**
     * Handle duplicate definition errors (MSG-008).
     */
    @ExceptionHandler(DuplicateDefinitionException.class)
    public ResponseEntity<DefinitionErrorResponse> handleDuplicateDefinition(DuplicateDefinitionException ex) {
        log.warn("Duplicate definition: {}", ex.getMessage());
        return definitionError(HttpStatus.CONFLICT, ErrorCode.MSG_008);
    }

    /**
     * Handle definition not found errors (MSG-009).
     */
    @ExceptionHandler(DefinitionNotFoundException.class)
    public ResponseEntity<DefinitionErrorResponse> handleDefinitionNotFound(DefinitionNotFoundException ex) {
        log.warn("Definition not found: {}", ex.getMessage());
        return definitionError(HttpStatus.NOT_FOUND, ErrorCode.MSG_009);
    }

    /**
     * Handle invalid JSON format errors.
     */
    @ExceptionHandler(InvalidJsonException.class)
    public ResponseEntity<DefinitionErrorResponse> handleInvalidJson(InvalidJsonException ex) {
        log.warn("Invalid JSON: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new DefinitionErrorResponse(ErrorCode.MSG_001.getCode(), ex.getMessage()));
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

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failureResponse(List.of(error)));
    }

    private static MessageResponse failureResponse(List<ValidationError> errors) {
        return new MessageResponse(
            null,
            null,
            null,
            MessageStatus.VALIDATION_FAILED,
            LocalDateTime.now(),
            ValidationResultEnum.FAILED,
            errors
        );
    }

    private static ResponseEntity<DefinitionErrorResponse> definitionError(HttpStatus status, ErrorCode code) {
        return ResponseEntity.status(status)
                .body(new DefinitionErrorResponse(code.getCode(), code.getPersianMessage()));
    }
}
