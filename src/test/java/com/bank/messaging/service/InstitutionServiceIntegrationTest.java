package com.bank.messaging.service;

import com.bank.messaging.dto.InstitutionRequest;
import com.bank.messaging.dto.InstitutionResponse;
import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.entity.Institution;
import com.bank.messaging.exception.DuplicateInstitutionException;
import com.bank.messaging.exception.InstitutionNotFoundException;
import com.bank.messaging.repository.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for InstitutionService.
 * Tests CRUD operations and institution validation logic.
 */
@SpringBootTest
@Transactional
class InstitutionServiceIntegrationTest {

    @Autowired
    private InstitutionService service;

    @Autowired
    private InstitutionRepository repository;

    private InstitutionRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new InstitutionRequest(
            "BANK01", "BANK01XXX", "Bank One", "BR001", true, List.of("SWIFT", "SEPA"));
    }

    @Nested
    @DisplayName("Create Institution Tests")
    class CreateInstitutionTests {

        @Test
        @DisplayName("createInstitution_validRequest_returnsCreated")
        void createInstitution_validRequest_returnsCreated() {
            // Given
            InstitutionRequest request = validRequest;

            // When
            InstitutionResponse response = service.createInstitution(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.institutionId()).isEqualTo("BANK01");
            assertThat(response.name()).isEqualTo("Bank One");
            assertThat(response.bic()).isEqualTo("BANK01XXX");
            assertThat(response.branchId()).isEqualTo("BR001");
            assertThat(response.isActive()).isTrue();
            assertThat(response.supportedNetworks()).containsExactly("SWIFT", "SEPA");
        }

        @Test
        @DisplayName("createInstitution_duplicate_throwsException")
        void createInstitution_duplicate_throwsException() {
            // Given
            service.createInstitution(validRequest);

            // When & Then
            assertThatThrownBy(() -> service.createInstitution(validRequest))
                .isInstanceOf(DuplicateInstitutionException.class)
                .hasMessageContaining("BANK01");
        }
    }

    @Nested
    @DisplayName("Get Institution Tests")
    class GetInstitutionTests {

        @Test
        @DisplayName("getInstitution_existing_returnsInstitution")
        void getInstitution_existing_returnsInstitution() {
            // Given
            InstitutionResponse created = service.createInstitution(validRequest);

            // When
            InstitutionResponse response = service.getInstitution(created.id());

            // Then
            assertThat(response.id()).isEqualTo(created.id());
            assertThat(response.institutionId()).isEqualTo("BANK01");
        }

        @Test
        @DisplayName("getInstitution_nonExistent_throwsException")
        void getInstitution_nonExistent_throwsException() {
            // Given
            long nonExistentId = 9999L;

            // When & Then
            assertThatThrownBy(() -> service.getInstitution(nonExistentId))
                .isInstanceOf(InstitutionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("List Institutions Tests")
    class ListInstitutionsTests {

        @Test
        @DisplayName("listInstitutions_noFilter_returnsAll")
        void listInstitutions_noFilter_returnsAll() {
            // Given
            service.createInstitution(validRequest);
            service.createInstitution(new InstitutionRequest(
                "BANK02", "BANK02XXX", "Bank Two", "BR002", true, List.of("SWIFT")));

            // When
            List<InstitutionResponse> institutions = service.listInstitutions(null, null, null);

            // Then
            assertThat(institutions).hasSize(2);
        }

        @Test
        @DisplayName("listInstitutions_filteredByName_returnsFiltered")
        void listInstitutions_filteredByName_returnsFiltered() {
            // Given
            service.createInstitution(validRequest);
            service.createInstitution(new InstitutionRequest(
                "BANK02", "BANK02XXX", "Another Bank", "BR002", true, List.of("SWIFT")));

            // When
            List<InstitutionResponse> institutions = service.listInstitutions("Bank", null, null);

            // Then
            assertThat(institutions).hasSize(1);
            assertThat(institutions.get(0).name()).isEqualTo("Bank One");
        }

        @Test
        @DisplayName("listInstitutions_filteredByNetwork_returnsFiltered")
        void listInstitutions_filteredByNetwork_returnsFiltered() {
            // Given
            service.createInstitution(validRequest);
            service.createInstitution(new InstitutionRequest(
                "BANK02", "BANK02XXX", "Bank Two", "BR002", true, List.of("SEPA")));

            // When
            List<InstitutionResponse> institutions = service.listInstitutions(null, "SWIFT", null);

            // Then
            assertThat(institutions).hasSize(1);
            assertThat(institutions.get(0).institutionId()).isEqualTo("BANK01");
        }

        @Test
        @DisplayName("listInstitutions_filteredByActive_returnsFiltered")
        void listInstitutions_filteredByActive_returnsFiltered() {
            // Given
            service.createInstitution(validRequest);
            service.createInstitution(new InstitutionRequest(
                "BANK02", "BANK02XXX", "Inactive Bank", "BR002", false, List.of("SWIFT")));

            // When
            List<InstitutionResponse> institutions = service.listInstitutions(null, null, true);

            // Then
            assertThat(institutions).hasSize(1);
            assertThat(institutions.get(0).isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Update Institution Tests")
    class UpdateInstitutionTests {

        @Test
        @DisplayName("updateInstitution_existing_returnsUpdated")
        void updateInstitution_existing_returnsUpdated() {
            // Given
            InstitutionResponse created = service.createInstitution(validRequest);

            // When
            InstitutionRequest updateRequest = new InstitutionRequest(
                "BANK01", "BANK01XXX", "Updated Bank", "BR001", false, List.of("SWIFT"));
            InstitutionResponse response = service.updateInstitution(created.id(), updateRequest);

            // Then
            assertThat(response.id()).isEqualTo(created.id());
            assertThat(response.name()).isEqualTo("Updated Bank");
            assertThat(response.isActive()).isFalse();
        }

        @Test
        @DisplayName("updateInstitution_nonExistent_throwsException")
        void updateInstitution_nonExistent_throwsException() {
            // Given
            long nonExistentId = 9999L;
            InstitutionRequest request = new InstitutionRequest(
                "BANK01", "BANK01XXX", "Bank One", "BR001", true, List.of("SWIFT"));

            // When & Then
            assertThatThrownBy(() -> service.updateInstitution(nonExistentId, request))
                .isInstanceOf(InstitutionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Institution Tests")
    class DeleteInstitutionTests {

        @Test
        @DisplayName("deleteInstitution_existing_setsInactive")
        void deleteInstitution_existing_setsInactive() {
            // Given
            InstitutionResponse created = service.createInstitution(validRequest);

            // When
            service.deleteInstitution(created.id());

            // Then
            Optional<Institution> byId = repository.findById(created.id());
            assertThat(byId).isPresent();
            assertThat(byId.get().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("deleteInstitution_nonExistent_throwsException")
        void deleteInstitution_nonExistent_throwsException() {
            // Given
            long nonExistentId = 9999L;

            // When & Then
            assertThatThrownBy(() -> service.deleteInstitution(nonExistentId))
                .isInstanceOf(InstitutionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Find Institution by ID Tests")
    class FindInstitutionByIdTests {

        @Test
        @DisplayName("findByInstitutionId_existing_returnsInstitution")
        void findByInstitutionId_existing_returnsInstitution() {
            // Given
            service.createInstitution(validRequest);

            // When
            Optional<Institution> result = service.findByInstitutionId("BANK01");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getInstitutionId()).isEqualTo("BANK01");
        }

        @Test
        @DisplayName("findByInstitutionId_notFound_returnsEmpty")
        void findByInstitutionId_notFound_returnsEmpty() {
            // When
            Optional<Institution> result = service.findByInstitutionId("UNKNOWN");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validate Institution Tests")
    class ValidateInstitutionTests {

        @Test
        @DisplayName("validateInstitution_valid_returnsSuccess")
        void validateInstitution_valid_returnsSuccess() {
            // Given
            service.createInstitution(validRequest);

            // When
            ValidationResult result = service.validateInstitution("BANK01", "SWIFT");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("validateInstitution_notFound_returnsFailed")
        void validateInstitution_notFound_returnsFailed() {
            // When
            ValidationResult result = service.validateInstitution("UNKNOWN", "SWIFT");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).code()).isEqualTo("MSG-010");
        }

        @Test
        @DisplayName("validateInstitution_inactive_returnsFailed")
        void validateInstitution_inactive_returnsFailed() {
            // Given
            InstitutionRequest inactiveRequest = new InstitutionRequest(
                "BANK01", "BANK01XXX", "Inactive Bank", "BR001", false, List.of("SWIFT"));
            service.createInstitution(inactiveRequest);

            // When
            ValidationResult result = service.validateInstitution("BANK01", "SWIFT");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).code()).isEqualTo("MSG-011");
        }

        @Test
        @DisplayName("validateInstitution_networkNotSupported_returnsFailed")
        void validateInstitution_networkNotSupported_returnsFailed() {
            // Given
            InstitutionRequest request = new InstitutionRequest(
                "BANK01", "BANK01XXX", "Bank One", "BR001", true, List.of("SEPA"));
            service.createInstitution(request);

            // When
            ValidationResult result = service.validateInstitution("BANK01", "SWIFT");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).code()).isEqualTo("MSG-012");
        }
    }
}
