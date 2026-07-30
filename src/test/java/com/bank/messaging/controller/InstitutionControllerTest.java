package com.bank.messaging.controller;

import com.bank.messaging.dto.InstitutionRequest;
import com.bank.messaging.dto.InstitutionResponse;
import com.bank.messaging.entity.Institution;
import com.bank.messaging.exception.DuplicateInstitutionException;
import com.bank.messaging.exception.InstitutionNotFoundException;
import com.bank.messaging.service.InstitutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.bank.messaging.config.SecurityConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for InstitutionController (EPIC-03).
 * Covers L-AC-001 through L-AC-008 at the HTTP layer.
 */
@WebMvcTest(InstitutionController.class)
@Import(SecurityConfig.class)
class InstitutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InstitutionService institutionService;

    // Provides the JwtDecoder bean that SecurityConfig's OAuth2 resource server
    // (oauth2ResourceServer().jwt(...)) requires to build the SecurityFilterChain.
    // No test sends a real bearer token, so the mock is never invoked; it exists
    // only so the imported production SecurityConfig loads cleanly in this slice
    // (L-AC-008: unauthenticated requests must reach the entry point and return 401).
    @MockBean
    private JwtDecoder jwtDecoder;

    private final InstitutionResponse sampleResponse = new InstitutionResponse(
            1L, "BANK01", "BANK01XXX", "Bank One",
            "BR001", true, List.of("SWIFT", "SEPA"),
            LocalDateTime.now(), LocalDateTime.now());

    @Nested
    @DisplayName("POST /api/v1/institutions")
    class CreateInstitutionTests {

        @Test
        @WithMockUser
        void postInstitution_valid_returns201() throws Exception {
            InstitutionRequest request = new InstitutionRequest(
                    "BANK01", "BANK01XXX", "Bank One", "BR001", true, List.of("SWIFT", "SEPA"));

            when(institutionService.createInstitution(any())).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/institutions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.institutionId").value("BANK01"))
                .andExpect(jsonPath("$.name").value("Bank One"));
        }

        @Test
        @WithMockUser
        void postInstitution_invalidBody_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/institutions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        void postInstitution_duplicate_returns409() throws Exception {
            InstitutionRequest request = new InstitutionRequest(
                    "BANK01", "BANK01XXX", "Bank One", "BR001", true, List.of("SWIFT", "SEPA"));

            when(institutionService.createInstitution(any()))
                    .thenThrow(new DuplicateInstitutionException("BANK01"));

            mockMvc.perform(post("/api/v1/institutions")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/institutions/{id}")
    class GetInstitutionTests {

        @Test
        @WithMockUser
        void getInstitution_existing_returns200() throws Exception {
            when(institutionService.getInstitution(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/institutions/1")
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.institutionId").value("BANK01"));
        }

        @Test
        @WithMockUser
        void getInstitution_nonExistent_returns404() throws Exception {
            when(institutionService.getInstitution(999L))
                    .thenThrow(new InstitutionNotFoundException("999"));

            mockMvc.perform(get("/api/v1/institutions/999")
                    .with(csrf()))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/institutions")
    class ListInstitutionsTests {

        @Test
        @WithMockUser
        void getInstitutions_noFilter_returns200() throws Exception {
            when(institutionService.listInstitutions(null, null, null))
                    .thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/institutions")
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].institutionId").value("BANK01"));
        }

        @Test
        @WithMockUser
        void getInstitutions_filtered_returns200() throws Exception {
            when(institutionService.listInstitutions(null, "SWIFT", null))
                    .thenReturn(List.of(sampleResponse));

            mockMvc.perform(get("/api/v1/institutions")
                    .with(csrf())
                    .param("network", "SWIFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/institutions/{id}")
    class UpdateInstitutionTests {

        @Test
        @WithMockUser
        void putInstitution_existing_returns200() throws Exception {
            InstitutionRequest request = new InstitutionRequest(
                    "BANK01", "BANK01XXX", "Bank One Updated", "BR001", true, List.of("SWIFT"));

            when(institutionService.updateInstitution(eq(1L), any())).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/institutions/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockUser
        void putInstitution_nonExistent_returns404() throws Exception {
            InstitutionRequest request = new InstitutionRequest(
                    "BANK01", "BANK01XXX", "Bank One Updated", "BR001", true, List.of("SWIFT"));

            when(institutionService.updateInstitution(eq(999L), any()))
                    .thenThrow(new InstitutionNotFoundException("999"));

            mockMvc.perform(put("/api/v1/institutions/999")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/institutions/{id}")
    class DeleteInstitutionTests {

        @Test
        @WithMockUser
        void deleteInstitution_existing_returns204() throws Exception {
            mockMvc.perform(delete("/api/v1/institutions/1")
                    .with(csrf()))
                .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        void deleteInstitution_nonExistent_returns404() throws Exception {
            doThrow(new InstitutionNotFoundException("999"))
                    .when(institutionService).deleteInstitution(999L);

            mockMvc.perform(delete("/api/v1/institutions/999")
                    .with(csrf()))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/institutions/lookup")
    class LookupInstitutionTests {

        @Test
        @WithMockUser
        void lookupInstitution_found_returns200() throws Exception {
            Institution entity = Institution.builder()
                    .id(1L).institutionId("BANK01").name("Bank One").isActive(true)
                    .build();
            when(institutionService.findByInstitutionId("BANK01"))
                    .thenReturn(Optional.of(entity));

            mockMvc.perform(get("/api/v1/institutions/lookup")
                    .with(csrf())
                    .param("institutionId", "BANK01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.institutionId").value("BANK01"));
        }

        @Test
        @WithMockUser
        void lookupInstitution_notFound_returns404() throws Exception {
            when(institutionService.findByInstitutionId("UNKNOWN"))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/institutions/lookup")
                    .with(csrf())
                    .param("institutionId", "UNKNOWN"))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Unauthorized Access Tests (L-AC-008)")
    class UnauthorizedAccessTests {

        @Test
        void postInstitution_unauthorized_returns401() throws Exception {
            InstitutionRequest request = new InstitutionRequest(
                    "BANK01", "BANK01XXX", "Bank One", "BR001", true, List.of("SWIFT", "SEPA"));

            mockMvc.perform(post("/api/v1/institutions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void getInstitutions_unauthorized_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/institutions"))
                .andExpect(status().isUnauthorized());
        }
    }
}
