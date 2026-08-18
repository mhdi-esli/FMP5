package com.bank.messaging.service;

import com.bank.messaging.dto.MessageRequest;
import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.enums.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MessageValidationService.
 * Tests AC-002, AC-003, AC-004 validation scenarios.
 */
@ExtendWith(MockitoExtension.class)
class MessageValidationServiceTest {

    @InjectMocks
    private MessageValidationService validationService;

    private MessageRequest.MessageRequestBuilder validRequestBuilder;

    @BeforeEach
    void setUp() {
        validRequestBuilder = MessageRequest.builder()
            .messageType("200")
            .network("SWIFT")
            .requestReference("REQ-10001")
            .transactionReference("TRX-458796")
            .valueDate("2026-07-25")
            .currency("USD")
            .amount(BigDecimal.valueOf(25000))
            .senderInstitutionIdentifier("BANK01")
            .receiverInstitutionIdentifier("BANK02");
    }

    @Nested
    @DisplayName("AC-001: Valid Request Tests")
    class ValidRequestTests {

        @Test
        @DisplayName("validate_allFieldsPresent_returnsSuccess")
        void validate_allFieldsPresent_returnsSuccess() {
            // Given
            MessageRequest request = validRequestBuilder.build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("AC-002: Invalid Amount Tests")
    class InvalidAmountTests {

        @Test
        @DisplayName("validate_amountZero_returnsFailed")
        void validate_amountZero_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .amount(BigDecimal.ZERO)
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);

            ValidationError error = result.errors().get(0);
            assertThat(error.code()).isEqualTo(ErrorCode.MSG_001.getCode());
            assertThat(error.field()).isEqualTo("amount");
            assertThat(error.message()).contains("صفر");
        }

        @Test
        @DisplayName("validate_amountNegative_returnsFailed")
        void validate_amountNegative_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .amount(BigDecimal.valueOf(-100))
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);

            ValidationError error = result.errors().get(0);
            assertThat(error.code()).isEqualTo(ErrorCode.MSG_001.getCode());
            assertThat(error.field()).isEqualTo("amount");
        }
    }

    @Nested
    @DisplayName("AC-003: Missing Required Field Tests")
    class MissingRequiredFieldTests {

        @Test
        @DisplayName("validate_missingCurrency_returnsFailed")
        void validate_missingCurrency_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .currency(null)
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).isNotEmpty();
            assertThat(result.errors())
                .anyMatch(e -> e.code().equals(ErrorCode.MSG_001.getCode()));

            // Alternative invalid currency also returns MSG-001
            MessageRequest invalidCurrencyRequest = validRequestBuilder
                .currency("INVALID")
                .build();

            ValidationResult invalidResult = validationService.validate(invalidCurrencyRequest);
            assertThat(invalidResult.errors())
                .anyMatch(e -> e.code().equals(ErrorCode.MSG_001.getCode())
                    && e.field().equals("currency"));
        }

        @Test
        @DisplayName("validate_invalidCurrencyCode_returnsFailed")
        void validate_invalidCurrencyCode_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .currency("INVALID")
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);

            ValidationError error = result.errors().get(0);
            assertThat(error.code()).isEqualTo(ErrorCode.MSG_001.getCode());
            assertThat(error.field()).isEqualTo("currency");
        }

        @Test
        @DisplayName("validate_invalidValueDateFormat_returnsFailed")
        void validate_invalidValueDateFormat_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .valueDate("2026/07/25") // Wrong format
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);

            ValidationError error = result.errors().get(0);
            assertThat(error.code()).isEqualTo(ErrorCode.MSG_001.getCode());
            assertThat(error.field()).isEqualTo("valueDate");
        }
    }

    @Nested
    @DisplayName("AC-004: Unsupported Network Tests")
    class UnsupportedNetworkTests {

        @Test
        @DisplayName("validate_invalidNetwork_returnsFailed")
        void validate_invalidNetwork_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .network("INVALID_NETWORK")
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);

            ValidationError error = result.errors().get(0);
            assertThat(error.code()).isEqualTo(ErrorCode.MSG_003.getCode());
            assertThat(error.field()).isEqualTo("network");
            assertThat(error.message()).isEqualTo(ErrorCode.MSG_003.getPersianMessage());
        }

        @Test
        @DisplayName("validate_swiftNetwork_returnsSuccess")
        void validate_swiftNetwork_returnsSuccess() {
            // Given
            MessageRequest request = validRequestBuilder
                .network("SWIFT")
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("validate_sepaNetwork_returnsSuccess")
        void validate_sepaNetwork_returnsSuccess() {
            // Given
            MessageRequest request = validRequestBuilder
                .network("SEPA")
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Multiple Errors Tests")
    class MultipleErrorsTests {

        @Test
        @DisplayName("validate_multipleErrors_returnsAllErrors")
        void validate_multipleErrors_returnsAllErrors() {
            // Given
            MessageRequest request = validRequestBuilder
                .amount(BigDecimal.ZERO)
                .currency("INVALID")
                .network("INVALID_NETWORK")
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(3);

            List<String> fields = result.errors().stream()
                .map(ValidationError::field)
                .toList();
            assertThat(fields).containsExactlyInAnyOrder("amount", "currency", "network");
        }
    }
}
