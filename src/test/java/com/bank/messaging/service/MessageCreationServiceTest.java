package com.bank.messaging.service;

import com.bank.messaging.dto.MessageRequest;
import com.bank.messaging.dto.MessageResponse;
import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.entity.MessageDefinitionMapping;
import com.bank.messaging.enums.ErrorCode;
import com.bank.messaging.enums.MessageStatus;
import com.bank.messaging.enums.ValidationResultEnum;
import com.bank.messaging.repository.MessageDefinitionMappingRepository;
import com.bank.messaging.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MessageCreationService.
 * Tests AC-001, AC-005 scenarios.
 */
@ExtendWith(MockitoExtension.class)
class MessageCreationServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageValidationService validationService;

    @Mock
    private MessageDefinitionMappingRepository messageDefinitionRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MessageCreationService messageCreationService;

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
    @DisplayName("AC-001: Create Valid Message Tests")
    class CreateValidMessageTests {

        @Test
        @DisplayName("createMessage_validRequest_returnsDraftStatus")
        void createMessage_validRequest_returnsDraftStatus() {
            // Given
            MessageRequest request = validRequestBuilder.build();

            when(validationService.validate(any())).thenReturn(ValidationResult.success());
            when(messageDefinitionRepository.findByMessageTypeAndNetworkAndIsActiveTrue("200", "SWIFT"))
                .thenReturn(Optional.of(MessageDefinitionMapping.builder()
                    .messageType("200")
                    .network("SWIFT")
                    .isActive(true)
                    .build()));
            when(messageRepository.save(any())).thenAnswer(invocation -> {
                Message msg = invocation.getArgument(0);
                msg.setMessageId("MSG-20260726-000001");
                return msg;
            });

            // When
            MessageResponse response = messageCreationService.createMessage(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(MessageStatus.DRAFT);
            assertThat(response.validationResult()).isEqualTo(ValidationResultEnum.SUCCESS);
            assertThat(response.messageId()).isNotNull();
            assertThat(response.messageId()).startsWith("MSG-");
        }

        @Test
        @DisplayName("createMessage_generatesUniqueMessageId")
        void createMessage_generatesUniqueMessageId() {
            // Given
            MessageRequest request = validRequestBuilder.build();

            when(validationService.validate(any())).thenReturn(ValidationResult.success());
            when(messageDefinitionRepository.findByMessageTypeAndNetworkAndIsActiveTrue("200", "SWIFT"))
                .thenReturn(Optional.of(MessageDefinitionMapping.builder().build()));
            when(messageRepository.save(any())).thenAnswer(invocation -> {
                Message msg = invocation.getArgument(0);
                return msg;
            });

            // When
            MessageResponse response1 = messageCreationService.createMessage(request);
            MessageResponse response2 = messageCreationService.createMessage(request);

            // Then
            assertThat(response1.messageId()).isNotNull();
            assertThat(response2.messageId()).isNotNull();
            assertThat(response1.messageId()).isNotEqualTo(response2.messageId());
        }

        @Test
        @DisplayName("createMessage_messageIdFormat_correct")
        void createMessage_messageIdFormat_correct() {
            // Given
            MessageRequest request = validRequestBuilder.build();

            when(validationService.validate(any())).thenReturn(ValidationResult.success());
            when(messageDefinitionRepository.findByMessageTypeAndNetworkAndIsActiveTrue("200", "SWIFT"))
                .thenReturn(Optional.of(MessageDefinitionMapping.builder().build()));
            when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MessageResponse response = messageCreationService.createMessage(request);

            // Then
            assertThat(response.messageId()).matches("MSG-\\d{8}-\\d{6}");
        }
    }

    @Nested
    @DisplayName("AC-002: Validation Failure Tests")
    class ValidationFailureTests {

        @Test
        @DisplayName("createMessage_validationFailed_returnsValidationFailedStatus")
        void createMessage_validationFailed_returnsValidationFailedStatus() {
            // Given
            MessageRequest request = validRequestBuilder
                .amount(BigDecimal.ZERO)
                .build();

            when(validationService.validate(any())).thenReturn(ValidationResult.failed(
                new ValidationError(ErrorCode.MSG_001.getCode(), "amount", "مبلغ باید بزرگتر از صفر باشد")
            ));
            when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MessageResponse response = messageCreationService.createMessage(request);

            // Then
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationResult()).isEqualTo(ValidationResultEnum.FAILED);
            assertThat(response.validationErrors()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("AC-005: Missing Message Definition Tests")
    class MissingMessageDefinitionTests {

        @Test
        @DisplayName("createMessage_messageDefinitionNotFound_returnsError")
        void createMessage_messageDefinitionNotFound_returnsError() {
            // Given
            MessageRequest request = validRequestBuilder.build();

            when(validationService.validate(any())).thenReturn(ValidationResult.success());
            when(messageDefinitionRepository.findByMessageTypeAndNetworkAndIsActiveTrue("200", "SWIFT"))
                .thenReturn(Optional.empty());
            when(messageDefinitionRepository.findByMessageTypeAndNetwork("200", "SWIFT"))
                .thenReturn(Optional.empty());
            when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MessageResponse response = messageCreationService.createMessage(request);

            // Then
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationErrors()).hasSize(1);
            assertThat(response.validationErrors().get(0).code()).isEqualTo(ErrorCode.MSG_004.getCode());
        }

        @Test
        @DisplayName("createMessage_messageDefinitionInactive_returnsError")
        void createMessage_messageDefinitionInactive_returnsError() {
            // Given
            MessageRequest request = validRequestBuilder.build();

            when(validationService.validate(any())).thenReturn(ValidationResult.success());
            when(messageDefinitionRepository.findByMessageTypeAndNetworkAndIsActiveTrue("200", "SWIFT"))
                .thenReturn(Optional.empty());
            when(messageDefinitionRepository.findByMessageTypeAndNetwork("200", "SWIFT"))
                .thenReturn(Optional.of(MessageDefinitionMapping.builder()
                    .messageType("200")
                    .network("SWIFT")
                    .isActive(false)
                    .build()));
            when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MessageResponse response = messageCreationService.createMessage(request);

            // Then
            assertThat(response.status()).isEqualTo(MessageStatus.VALIDATION_FAILED);
            assertThat(response.validationErrors()).hasSize(1);
            assertThat(response.validationErrors().get(0).code()).isEqualTo(ErrorCode.MSG_005.getCode());
        }
    }
}
