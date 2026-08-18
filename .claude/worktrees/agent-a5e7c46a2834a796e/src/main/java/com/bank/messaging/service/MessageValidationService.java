package com.bank.messaging.service;

import com.bank.messaging.dto.MessageRequest;
import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.enums.ErrorCode;
import com.bank.messaging.enums.Network;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for validating message creation requests.
 * Collects all validation errors and returns them together.
 */
@Service
@Slf4j
public class MessageValidationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
        "USD", "EUR", "GBP", "CHF", "JPY", "CAD", "AUD", "NZD", "SGD", "HKD",
        "NOK", "SEK", "DKK", "KWD", "BHD", "SAR", "AED", "QAR", "OMR", "JOD",
        "EGP", "LBP", "SYP", "IQD", "IRR", "AFN", "PKR", "INR", "CNY"
    );

    /**
     * Validates the message request.
     * Collects all errors and returns them together.
     *
     * @param request the message request to validate
     * @return validation result with any errors found
     */
    public ValidationResult validate(MessageRequest request) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate network
        validateNetwork(request.network(), errors);

        // Validate amount
        validateAmount(request.amount(), errors);

        // Validate currency
        validateCurrency(request.currency(), errors);

        // Validate value date
        validateValueDate(request.valueDate(), errors);

        // Validate references for invalid characters
        validateReference(request.requestReference(), "requestReference", errors);
        validateReference(request.transactionReference(), "transactionReference", errors);
        if (request.relatedReference() != null) {
            validateReference(request.relatedReference(), "relatedReference", errors);
        }

        if (!errors.isEmpty()) {
            log.warn("Validation failed with {} errors", errors.size());
            return ValidationResult.failed(errors);
        }

        log.debug("Validation passed");
        return ValidationResult.success();
    }

    private void validateNetwork(String network, List<ValidationError> errors) {
        if (network == null || network.isBlank()) {
            return; // @NotBlank handles this
        }

        Network parsedNetwork = Network.fromCode(network);
        if (parsedNetwork == null) {
            errors.add(new ValidationError(
                ErrorCode.MSG_003.getCode(),
                "network",
                ErrorCode.MSG_003.getPersianMessage()
            ));
        }
    }

    private void validateAmount(BigDecimal amount, List<ValidationError> errors) {
        if (amount == null) {
            return; // @NotNull handles this
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationError(
                ErrorCode.MSG_001.getCode(),
                "amount",
                "مبلغ باید بزرگتر از صفر باشد"
            ));
        }
    }

    private void validateCurrency(String currency, List<ValidationError> errors) {
        if (currency == null || currency.isBlank()) {
            errors.add(new ValidationError(
                ErrorCode.MSG_001.getCode(),
                "currency",
                "کد ارز الزامی است"
            ));
            return;
        }

        if (!SUPPORTED_CURRENCIES.contains(currency.toUpperCase())) {
            errors.add(new ValidationError(
                ErrorCode.MSG_001.getCode(),
                "currency",
                "کد ارز معتبر نیست"
            ));
        }
    }

    private void validateValueDate(String valueDate, List<ValidationError> errors) {
        if (valueDate == null || valueDate.isBlank()) {
            return; // @NotBlank handles this
        }

        try {
            LocalDate parsedDate = LocalDate.parse(valueDate, DATE_FORMATTER);
            // Optionally check if date is not too far in the past or future
        } catch (DateTimeParseException e) {
            errors.add(new ValidationError(
                ErrorCode.MSG_001.getCode(),
                "valueDate",
                "فرمت تاریخ معتبر نیست (YYYY-MM-DD)"
            ));
        }
    }

    private void validateReference(String reference, String fieldName, List<ValidationError> errors) {
        if (reference == null || reference.isBlank()) {
            return;
        }

        // Check for invalid characters (allow alphanumeric, hyphens, underscores)
        if (!reference.matches("^[a-zA-Z0-9\\-_]+$")) {
            errors.add(new ValidationError(
                ErrorCode.MSG_001.getCode(),
                fieldName,
                "مرجع شامل کاراکترهای غیرمجاز است"
            ));
        }
    }
}
