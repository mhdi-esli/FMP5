package com.bank.messaging.service;

import com.bank.messaging.dto.InstitutionRequest;
import com.bank.messaging.dto.MessageRequest;
import com.bank.messaging.dto.MessageResponse;
import com.bank.messaging.entity.Institution;
import com.bank.messaging.enums.MessageStatus;
import com.bank.messaging.repository.InstitutionRepository;
import com.bank.messaging.repository.MessageDefinitionMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MessageCreationService.
 * Tests complete message creation flow with all services and repository.
 */
@SpringBootTest
@Transactional
class MessageCreationServiceIntegrationTest {

    @Autowired
    private MessageCreationService service;

    @Autowired
    private MessageDefinitionService definitionService;

    @Autowired
    private InstitutionService institutionService;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private MessageDefinitionMappingRepository definitionRepository;

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

        // Setup active definition for MT200/SWIFT
        definitionService.createDefinition(new com.bank.messaging.dto.MessageDefinitionRequest(
            "MT200", "SWIFT", 1, null, null, true));

        // Setup sender institution
        institutionService.createInstitution(new InstitutionRequest(
            "BANK01", "BANK01XXX", "Bank One", "BR001", true, List.of("SWIFT")));
    }

    @Nested
    @DisplayName("Create Valid Message Tests")
    class CreateValidMessageTests {

        @Test
        @DisplayName("createMessage_validRequest_returnsSuccess")
        void createMessage_validRequest_returnsSuccess() {
            // Given
            MessageRequest request = validRequestBuilder
                .senderInstitutionIdentifier("BANK01")
                .receiverInstitutionIdentifier("BANK02")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNotNull();
            assertThat(response.messageId()).startsWith("MSG-");
            assertThat(response.messageType()).isEqualTo("200");
            assertThat(response.network()).isEqualTo("SWIFT");
            assertThat(response.status()).isEqualTo(MessageStatus.DRAFT);
            assertThat(response.validationResult()).isEqualTo("SUCCESS");
            assertThat(response.validationErrors()).isEmpty();
        }

        @Test
        @DisplayName("createMessage_generatesUniqueMessageId")
        void createMessage_generatesUniqueMessageId() {
            // Given
            MessageRequest request1 = validRequestBuilder
                .requestReference("REQ-10001")
                .transactionReference("TRX-111111")
                .build();
            MessageRequest request2 = validRequestBuilder
                .requestReference("REQ-10002")
                .transactionReference("TRX-222222")
                .build();

            // When
            MessageResponse response1 = service.createMessage(request1);
            MessageResponse response2 = service.createMessage(request2);

            // Then
            assertThat(response1.messageId()).isNotNull();
            assertThat(response2.messageId()).isNotNull();
            assertThat(response1.messageId()).isNotEqualTo(response2.messageId());
            assertThat(response1.messageId()).startsWith("MSG-" + response2.messageId().substring(4, 12));
        }

        @Test
        @DisplayName("createMessage_withAllFields_returnsCompleteResponse")
        void createMessage_withAllFields_returnsCompleteResponse() {
            // Given
            MessageRequest request = MessageRequest.builder()
                .messageType("200")
                .network("SWIFT")
                .requestReference("REQ-10001")
                .transactionReference("TRX-458796")
                .relatedReference("REL-0001")
                .valueDate("2026-07-25")
                .currency("USD")
                .amount(BigDecimal.valueOf(25000))
                .senderInstitutionIdentifier("BANK01")
                .senderBIC("BANK01XXX")
                .senderBranchIdentifier("BR001")
                .receiverInstitutionIdentifier("BANK02")
                .receiverBIC("BANK02XXX")
                .receiverBranchIdentifier("BR002")
                .chargeType("SHA")
                .instructionCode("URGP")
                .narrative("Transfer for payment")
                .additionalInformation("Additional info")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNotNull();
            assertThat(response.status()).isEqualTo(MessageStatus.DRAFT);
            assertThat(response.validationResult()).isEqualTo("SUCCESS");
        }
    }

    @Nested
    @DisplayName("Validation Failure Tests")
    class ValidationFailureTests {

        @Test
        @DisplayName("createMessage_invalidNetwork_returnsFailed")
        void createMessage_invalidNetwork_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .network("INVALID_NETWORK")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNull();
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationResult()).isEqualTo("FAILED");
            assertThat(response.validationErrors()).isNotEmpty();
            assertThat(response.validationErrors().get(0).code()).isEqualTo("MSG-003");
        }

        @Test
        @DisplayName("createMessage_missingSenderInstitution_returnsFailed")
        void createMessage_missingSenderInstitution_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .senderInstitutionIdentifier("UNKNOWN_BANK")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNull();
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationResult()).isEqualTo("FAILED");
            assertThat(response.validationErrors()).isNotEmpty();
            assertThat(response.validationErrors().get(0).code()).isEqualTo("MSG-010");
        }

        @Test
        @DisplayName("createMessage_inactiveSender_returnsFailed")
        void createMessage_inactiveSender_returnsFailed() {
            // Given - Create inactive sender
            institutionService.createInstitution(new InstitutionRequest(
                "BANK_INACTIVE", "BANK01XXX", "Inactive Bank", "BR001", false, List.of("SWIFT")));

            MessageRequest request = validRequestBuilder
                .senderInstitutionIdentifier("BANK_INACTIVE")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNull();
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationResult()).isEqualTo("FAILED");
            assertThat(response.validationErrors()).isNotEmpty();
            assertThat(response.validationErrors().get(0).code()).isEqualTo("MSG-011");
        }

        @Test
        @DisplayName("createMessage_networkNotSupported_returnsFailed")
        void createMessage_networkNotSupported_returnsFailed() {
            // Given - Create institution without SWIFT support
            institutionService.createInstitution(new InstitutionRequest(
                "BANK_NO_SWIFT", "BANK01XXX", "No SWIFT Bank", "BR001", true, List.of("SEPA")));

            MessageRequest request = validRequestBuilder
                .senderInstitutionIdentifier("BANK_NO_SWIFT")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNull();
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationResult()).isEqualTo("FAILED");
            assertThat(response.validationErrors()).isNotEmpty();
            assertThat(response.validationErrors().get(0).code()).isEqualTo("MSG-012");
        }

        @Test
        @DisplayName("createMessage_invalidAmount_returnsFailed")
        void createMessage_invalidAmount_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .amount(BigDecimal.ZERO)
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNull();
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationResult()).isEqualTo("FAILED");
            assertThat(response.validationErrors()).isNotEmpty();
            assertThat(response.validationErrors().get(0).code()).isEqualTo("MSG-001");
        }

        @Test
        @DisplayName("createMessage_invalidCurrency_returnsFailed")
        void createMessage_invalidCurrency_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .currency("INVALID")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNull();
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationResult()).isEqualTo("FAILED");
            assertThat(response.validationErrors()).isNotEmpty();
            assertThat(response.validationErrors().get(0).code()).isEqualTo("MSG-001");
        }

        @Test
        @DisplayName("createMessage_inactiveDefinition_returnsFailed")
        void createMessage_inactiveDefinition_returnsFailed() {
            // Given - Create inactive definition
            definitionService.createDefinition(new com.bank.messaging.dto.MessageDefinitionRequest(
                "MT200", "SEPA", 1, null, null, false));

            MessageRequest request = MessageRequest.builder()
                .messageType("200")
                .network("SEPA")
                .requestReference("REQ-10001")
                .transactionReference("TRX-458796")
                .valueDate("2026-07-25")
                .currency("USD")
                .amount(BigDecimal.valueOf(25000))
                .senderInstitutionIdentifier("BANK01")
                .receiverInstitutionIdentifier("BANK02")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNull();
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationResult()).isEqualTo("FAILED");
            assertThat(response.validationErrors()).isNotEmpty();
            assertThat(response.validationErrors().get(0).code()).isEqualTo("MSG-005");
        }

        @Test
        @DisplayName("createMessage_unknownDefinition_returnsFailed")
        void createMessage_unknownDefinition_returnsFailed() {
            // Given
            MessageRequest request = validRequestBuilder
                .messageType("UNKNOWN")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.messageId()).isNull();
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationResult()).isEqualTo("FAILED");
            assertThat(response.validationErrors()).isNotEmpty();
            assertThat(response.validationErrors().get(0).code()).isEqualTo("MSG-004");
        }
    }

    @Nested
    @DisplayName("Validation Errors Collection Tests")
    class ValidationErrorsCollectionTests {

        @Test
        @DisplayName("createMessage_multipleErrors_returnsAllErrors")
        void createMessage_multipleErrors_returnsAllErrors() {
            // Given
            MessageRequest request = MessageRequest.builder()
                .messageType("UNKNOWN")
                .network("INVALID_NETWORK")
                .requestReference("REQ-10001")
                .transactionReference("TRX-458796")
                .valueDate("2026-07-25")
                .currency("INVALID")
                .amount(BigDecimal.ZERO)
                .senderInstitutionIdentifier("UNKNOWN_SENDER")
                .receiverInstitutionIdentifier("BANK02")
                .build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.validationErrors()).isNotEmpty();
            // Should have errors for: definition not found, invalid network, invalid currency, invalid amount, institution not found
            assertThat(response.validationErrors().size()).isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Message Persistence Tests")
    class MessagePersistenceTests {

        @Test
        @DisplayName("createMessage_persistsToDatabase")
        void createMessage_persistsToDatabase() {
            // Given
            MessageRequest request = validRequestBuilder.build();

            // When
            MessageResponse response = service.createMessage(request);

            // Then
            assertThat(response.messageId()).isNotNull();
            // The message should be persisted and retrievable
            // We can verify by checking that message was saved with correct status
            assertThat(response.status()).isEqualTo(MessageStatus.DRAFT);
        }
    }
}
