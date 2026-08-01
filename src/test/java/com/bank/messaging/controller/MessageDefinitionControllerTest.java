package com.bank.messaging.controller;

import com.bank.messaging.dto.MessageDefinitionRequest;
import com.bank.messaging.dto.MessageDefinitionResponse;
import com.bank.messaging.exception.DefinitionNotFoundException;
import com.bank.messaging.exception.DuplicateDefinitionException;
import com.bank.messaging.service.MessageDefinitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageDefinitionController.class)
@Import(SecurityConfig.class)
class MessageDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private MessageDefinitionService definitionService;

    private final MessageDefinitionResponse sampleResponse = new MessageDefinitionResponse(
            1L, "MT200", "SWIFT", 1,
            "{\"field\": \"value\"}", "{\"rule\": \"active\"}",
            true, LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("POST /api/v1/message-definitions")
    class CreateDefinitionTests {

        @Test
        @WithMockUser
        void postMessageDefinition_valid_returns201() throws Exception {
            MessageDefinitionRequest request = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 1, null, null, true);

            when(definitionService.createDefinition(any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/message-definitions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.messageType").value("MT200"))
                .andExpect(jsonPath("$.network").value("SWIFT"));
        }

        @Test
        @WithMockUser
        void postMessageDefinition_duplicate_returns409() throws Exception {
            MessageDefinitionRequest request = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 1, null, null, true);

            when(definitionService.createDefinition(any()))
                    .thenThrow(new DuplicateDefinitionException("MT200", "SWIFT", 1));

            mockMvc.perform(post("/api/v1/message-definitions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MSG-008"));
        }

        @Test
        @WithMockUser
        void postMessageDefinition_noBody_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/message-definitions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/message-definitions")
    class ListDefinitionsTests {

        @Test
        @WithMockUser
        void getMessageDefinitions_noFilter_returns200() throws Exception {
            when(definitionService.listDefinitions(null, null, null))
                    .thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/message-definitions")
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].messageType").value("MT200"));
        }

        @Test
        @WithMockUser
        void getMessageDefinitions_filtered_returns200() throws Exception {
            when(definitionService.listDefinitions("MT200", null, null))
                    .thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/message-definitions")
                    .with(csrf())
                    .param("messageType", "MT200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/message-definitions/{id}")
    class GetDefinitionTests {

        @Test
        @WithMockUser
        void getMessageDefinition_existing_returns200() throws Exception {
            when(definitionService.getDefinition(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/message-definitions/1")
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockUser
        void getMessageDefinition_nonExistent_returns404() throws Exception {
            when(definitionService.getDefinition(999L))
                    .thenThrow(new DefinitionNotFoundException(999L));

            mockMvc.perform(get("/api/v1/message-definitions/999")
                    .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MSG-009"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/message-definitions/{id}")
    class UpdateDefinitionTests {

        @Test
        @WithMockUser
        void putMessageDefinition_existing_returns200() throws Exception {
            MessageDefinitionRequest request = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 2, null, null, true);

            when(definitionService.updateDefinition(eq(1L), any())).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/message-definitions/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockUser
        void putMessageDefinition_nonExistent_returns404() throws Exception {
            MessageDefinitionRequest request = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 2, null, null, true);

            when(definitionService.updateDefinition(eq(999L), any()))
                    .thenThrow(new DefinitionNotFoundException(999L));

            mockMvc.perform(put("/api/v1/message-definitions/999")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MSG-009"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/message-definitions/{id}")
    class DeleteDefinitionTests {

        @Test
        @WithMockUser
        void deleteMessageDefinition_existing_returns204() throws Exception {
            mockMvc.perform(delete("/api/v1/message-definitions/1")
                    .with(csrf()))
                .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        void deleteMessageDefinition_nonExistent_returns404() throws Exception {
            doThrow(new DefinitionNotFoundException(999L))
                    .when(definitionService).deleteDefinition(999L);

            mockMvc.perform(delete("/api/v1/message-definitions/999")
                    .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MSG-009"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/message-definitions/lookup")
    class LookupDefinitionTests {

        @Test
        @WithMockUser
        void lookupMessageDefinition_found_returns200() throws Exception {
            when(definitionService.findActiveDefinition("MT200", "SWIFT"))
                    .thenReturn(Optional.of(new com.bank.messaging.entity.MessageDefinitionMapping()));

            mockMvc.perform(get("/api/v1/message-definitions/lookup")
                    .with(csrf())
                    .param("messageType", "MT200")
                    .param("network", "SWIFT"))
                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        void lookupMessageDefinition_notFound_returns404() throws Exception {
            when(definitionService.findActiveDefinition("UNKNOWN", "SWIFT"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/message-definitions/lookup")
                    .with(csrf())
                    .param("messageType", "UNKNOWN")
                    .param("network", "SWIFT"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Unauthorized Access Tests")
    class UnauthorizedAccessTests {

        @Test
        void postMessageDefinition_noToken_returns401() throws Exception {
            MessageDefinitionRequest request = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 1, null, null, true);

            mockMvc.perform(post("/api/v1/message-definitions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void getMessageDefinitions_noToken_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/message-definitions"))
                .andExpect(status().isUnauthorized());
        }
    }
}
