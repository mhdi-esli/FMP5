package com.bank.messaging.controller;

import com.bank.messaging.dto.MessageRequest;
import com.bank.messaging.dto.MessageResponse;
import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.enums.MessageStatus;
import com.bank.messaging.enums.ValidationResultEnum;
import com.bank.messaging.service.MessageCreationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.bank.messaging.config.SecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for MessageController.
 * Tests AC-001, AC-003, AC-007 scenarios.
 */
@WebMvcTest(MessageController.class)
@Import(SecurityConfig.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
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
        @WithMockUser
        @DisplayName("postMessages_validRequest_returns200")
        void postMessages_validRequest_returns200() throws Exception {
            // Given
            MessageRequest request = validRequestBuilder.build();
            MessageResponse response = new MessageResponse(
                "MSG-20260726-000001",
                "200",
                "SWIFT",
                MessageStatus.DRAFT,
                LocalDateTime.now(),
                ValidationResultEnum.SUCCESS,
                Collections.emptyList()
            );

            when(messageCreationService.createMessage(any())).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/v1/messages")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value("MSG-20260726-000001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.validationResult").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("AC-003: Missing Required Field Tests")
    class MissingRequiredFieldTests {

        @Test
        @WithMockUser
        @DisplayName("postMessages_missingCurrency_returns400")
        void postMessages_missingCurrency_returns400() throws Exception {
            // Given
            String invalidRequest = """
                {
                    "messageType": "200",
                    "network": "SWIFT",
                    "requestReference": "REQ-10001",
                    "transactionReference": "TRX-458796",
                    "valueDate": "2026-07-25",
                    "amount": 25000,
                    "senderInstitutionIdentifier": "BANK01",
                    "receiverInstitutionIdentifier": "BANK02"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/v1/messages")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("AC-007: Unauthorized Access Tests")
    class UnauthorizedAccessTests {

        @Test
        @DisplayName("postMessages_noToken_returns401")
        void postMessages_noToken_returns401() throws Exception {
            // Given
            MessageRequest request = validRequestBuilder.build();

            // When & Then
            mockMvc.perform(post("/api/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("AC-002: Validation Failure Tests")
    class ValidationFailureTests {

        @Test
        @WithMockUser
        @DisplayName("postMessages_invalidAmount_returns400")
        void postMessages_invalidAmount_returns400() throws Exception {
            // Given
            MessageRequest request = validRequestBuilder
                .amount(BigDecimal.ZERO)
                .build();

            MessageResponse response = new MessageResponse(
                null,
                "200",
                "SWIFT",
                MessageStatus.VALIDATION_FAILED,
                LocalDateTime.now(),
                ValidationResultEnum.FAILED,
                List.of(new ValidationError("MSG-001", "amount", "مبلغ باید بزرگتر از صفر باشد"))
            );

            when(messageCreationService.createMessage(any())).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/v1/messages")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors[0].code").value("MSG-001"));
        }
    }

    @Nested
    @DisplayName("AC-004: Unsupported Network Tests")
    class UnsupportedNetworkTests {

        @Test
        @WithMockUser
        @DisplayName("postMessages_invalidNetwork_returns400")
        void postMessages_invalidNetwork_returns400() throws Exception {
            // Given
            MessageRequest request = validRequestBuilder
                .network("INVALID_NETWORK")
                .build();

            MessageResponse response = new MessageResponse(
                null,
                "200",
                "INVALID_NETWORK",
                MessageStatus.VALIDATION_FAILED,
                LocalDateTime.now(),
                ValidationResultEnum.FAILED,
                List.of(new ValidationError("MSG-003", "network", "شبکه انتخاب‌شده پشتیبانی نمی‌شود"))
            );

            when(messageCreationService.createMessage(any())).thenReturn(response);

            // When & Then
            mockMvc.perform(post("/api/v1/messages")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].code").value("MSG-003"));
        }
    }
}
