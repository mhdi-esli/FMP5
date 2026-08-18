package com.bank.messaging.controller;

import com.bank.messaging.config.SecurityConfig;
import com.bank.messaging.dto.MessageRequest;
import com.bank.messaging.entity.Message;
import com.bank.messaging.entity.MessageDefinitionMapping;
import com.bank.messaging.enums.ErrorCode;
import com.bank.messaging.repository.InstitutionRepository;
import com.bank.messaging.repository.MessageRepository;
import com.bank.messaging.service.InstitutionService;
import com.bank.messaging.service.MessageCreationService;
import com.bank.messaging.service.MessageDefinitionService;
import com.bank.messaging.service.MessageValidationService;
import com.bank.messaging.service.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP integration coverage for EPIC-03 AC-006.
 */
@WebMvcTest(MessageController.class)
@Import({SecurityConfig.class, MessageCreationService.class, InstitutionService.class})
class MessageControllerInstitutionValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessageRepository messageRepository;

    @MockBean
    private InstitutionRepository institutionRepository;

    @MockBean
    private MessageValidationService validationService;

    @MockBean
    private MessageDefinitionService messageDefinitionService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser
    void postMessages_missingSenderInstitution_returns400WithMsg010() throws Exception {
        MessageRequest request = MessageRequest.builder()
                .messageType("200")
                .network("SWIFT")
                .requestReference("REQ-10001")
                .transactionReference("TRX-458796")
                .valueDate("2026-07-25")
                .currency("USD")
                .amount(BigDecimal.valueOf(25000))
                .senderInstitutionIdentifier("UNKNOWN")
                .receiverInstitutionIdentifier("BANK02")
                .build();

        when(validationService.validate(any())).thenReturn(ValidationResult.success());
        when(messageDefinitionService.findActiveDefinition("200", "SWIFT"))
                .thenReturn(Optional.of(MessageDefinitionMapping.builder()
                        .messageType("200")
                        .network("SWIFT")
                        .isActive(true)
                        .build()));
        when(institutionRepository.findByInstitutionId("UNKNOWN")).thenReturn(Optional.empty());
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.<Message>getArgument(0));

        mockMvc.perform(post("/api/v1/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationResult").value("FAILED"))
                .andExpect(jsonPath("$.validationErrors.length()").value(1))
                .andExpect(jsonPath("$.validationErrors[0].code").value(ErrorCode.MSG_010.getCode()))
                .andExpect(jsonPath("$.validationErrors[0].field").value("senderInstitutionIdentifier"));
    }
}
