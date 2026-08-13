package com.bank.messaging.service;

import com.bank.messaging.dto.MessageRequest;
import com.bank.messaging.dto.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MessageValidationService.
 * Tests validation logic with real service instance.
 */
class MessageValidationServiceIntegrationTest {

    private final MessageValidationService validationService = new MessageValidationService();

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
    @DisplayName("Valid Request Tests")
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
    @DisplayName("Invalid Amount Tests")
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
            assertThat(error.code()).isEqualTo("MSG-001");
            assertThat(error.field()).isEqualTo("amount");
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
            assertThat(error.code()).isEqualTo("MSG-001");
            assertThat(error.field()).isEqualTo("amount");
        }
    }

    @Nested
    @DisplayName("Invalid Currency Tests")
    class InvalidCurrencyTests {

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
            assertThat(result.errors()).hasSize(1);

            ValidationError error = result.errors().get(0);
            assertThat(error.code()).isEqualTo("MSG-001");
            assertThat(error.field()).isEqualTo("currency");
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
            assertThat(error.code()).isEqualTo("MSG-001");
            assertThat(error.field()).isEqualTo("currency");
        }
    }

    @Nested
    @DisplayName("Invalid Network Tests")
    class InvalidNetworkTests {

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
            assertThat(error.code()).isEqualTo("MSG-003");
            assertThat(error.field()).isEqualTo("network");
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

    @Nested
    @DisplayName("Invalid Value Date Tests")
    class InvalidValueDateTests {

        @Test
        @DisplayName("validate_invalidValueDateFormat_returnsFailed")
        void validate_invalidValueDateFormat_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .valueDate("2026/07/25")
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);

            ValidationError error = result.errors().get(0);
            assertThat(error.code()).isEqualTo("MSG-001");
            assertThat(error.field()).isEqualTo("valueDate");
        }
    }

    @Nested
    @DisplayName("Invalid Reference Tests")
    class InvalidReferenceTests {

        @Test
        @DisplayName("validate_invalidRequestReference_returnsFailed")
        void validate_invalidRequestReference_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .requestReference("REQ_10001@INVALID")
                .build();

            // When
            ValidationResult result = validationService.validate(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);

            ValidationError error = result.errors().get(0);
            assertThat(error.code()).isEqualTo("MSG-001");
            assertThat(error.field()).isEqualTo("requestReference");
        }
    }
}
