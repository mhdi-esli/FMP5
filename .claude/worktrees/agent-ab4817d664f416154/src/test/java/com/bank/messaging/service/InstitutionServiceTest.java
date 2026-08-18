package com.bank.messaging.service;

import com.bank.messaging.dto.InstitutionRequest;
import com.bank.messaging.dto.InstitutionResponse;
import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.entity.Institution;
import com.bank.messaging.enums.ErrorCode;
import com.bank.messaging.exception.DuplicateInstitutionException;
import com.bank.messaging.exception.InstitutionNotFoundException;
import com.bank.messaging.repository.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InstitutionService (EPIC-03).
 * Covers L-AC-001 through L-AC-007 service-level scenarios.
 */
@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

    @Mock
    private InstitutionRepository repository;

    private InstitutionService service;

    private Institution sampleInstitution;
    private InstitutionRequest validRequest;

    @BeforeEach
    void setUp() {
        service = new InstitutionService(repository);
        sampleInstitution = Institution.builder()
                .id(1L)
                .institutionId("BANK01")
                .bic("BANK01XXX")
                .name("Bank One")
                .branchId("BR001")
                .isActive(true)
                .supportedNetworks(List.of("SWIFT", "SEPA"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validRequest = new InstitutionRequest(
                "BANK01", "BANK01XXX", "Bank One",
                "BR001", true, List.of("SWIFT", "SEPA"));
    }

    @Nested
    @DisplayName("Create Institution Tests (L-AC-001)")
    class CreateInstitutionTests {

        @Test
        void createInstitution_success_returnsCreatedInstitution() {
            when(repository.findByInstitutionId("BANK01")).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(sampleInstitution);

            InstitutionResponse response = service.createInstitution(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.institutionId()).isEqualTo("BANK01");
            assertThat(response.bic()).isEqualTo("BANK01XXX");
            assertThat(response.name()).isEqualTo("Bank One");
            assertThat(response.branchId()).isEqualTo("BR001");
            assertThat(response.isActive()).isTrue();
        }

        @Test
        void createInstitution_duplicateId_throwsException() {
            when(repository.findByInstitutionId("BANK01")).thenReturn(Optional.of(sampleInstitution));

            assertThatThrownBy(() -> service.createInstitution(validRequest))
                    .isInstanceOf(DuplicateInstitutionException.class)
                    .hasMessageContaining("BANK01");
        }

        @Test
        void createInstitution_withSupportedNetworks_storesNetworks() {
            when(repository.findByInstitutionId("BANK01")).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(sampleInstitution);

            InstitutionResponse response = service.createInstitution(validRequest);

            assertThat(response.supportedNetworks()).containsExactly("SWIFT", "SEPA");
        }

        @Test
        void createInstitution_withoutActive_defaultsToTrue() {
            InstitutionRequest requestNoActive = new InstitutionRequest(
                    "BANK02", "BANK02XXX", "Bank Two",
                    null, null, List.of("SWIFT"));

            when(repository.findByInstitutionId("BANK02")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(invocation -> {
                Institution saved = invocation.getArgument(0);
                saved.setId(2L);
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });

            InstitutionResponse response = service.createInstitution(requestNoActive);

            assertThat(response.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Get Institution Tests")
    class GetInstitutionTests {

        @Test
        void getInstitution_existingId_returnsInstitution() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleInstitution));

            InstitutionResponse response = service.getInstitution(1L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.institutionId()).isEqualTo("BANK01");
        }

        @Test
        void getInstitution_nonExistentId_throwsNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getInstitution(999L))
                    .isInstanceOf(InstitutionNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("List Institutions Tests (L-AC-003)")
    class ListInstitutionsTests {

        @Test
        void listInstitutions_noFilter_returnsAll() {
            when(repository.findAll()).thenReturn(List.of(sampleInstitution));

            List<InstitutionResponse> responses = service.listInstitutions(null, null, null);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).institutionId()).isEqualTo("BANK01");
        }

        @Test
        void listInstitutions_filterByName_returnsFiltered() {
            when(repository.findByNameContainingIgnoreCase("Bank One"))
                    .thenReturn(List.of(sampleInstitution));

            List<InstitutionResponse> responses = service.listInstitutions("Bank One", null, null);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).name()).isEqualTo("Bank One");
        }

        @Test
        void listInstitutions_filterByNetwork_returnsFiltered() {
            when(repository.findBySupportedNetwork("SWIFT"))
                    .thenReturn(List.of(sampleInstitution));

            List<InstitutionResponse> responses = service.listInstitutions(null, "SWIFT", null);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).supportedNetworks()).contains("SWIFT");
        }

        @Test
        void listInstitutions_filterByActive_returnsFiltered() {
            when(repository.findByIsActiveTrue()).thenReturn(List.of(sampleInstitution));

            List<InstitutionResponse> responses = service.listInstitutions(null, null, true);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).isActive()).isTrue();
        }

        @Test
        void listInstitutions_filterByInactive_returnsFiltered() {
            Institution inactive = Institution.builder()
                    .id(2L).institutionId("BANK02").name("Bank Two").isActive(false)
                    .build();
            when(repository.findByIsActiveFalse()).thenReturn(List.of(inactive));

            List<InstitutionResponse> responses = service.listInstitutions(null, null, false);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Update Institution Tests (L-AC-004)")
    class UpdateInstitutionTests {

        @Test
        void updateInstitution_success_updatesFields() {
            InstitutionRequest updateRequest = new InstitutionRequest(
                    "BANK01", "BANK01XXX", "Bank One Updated",
                    "BR002", false, List.of("SWIFT"));

            when(repository.findById(1L)).thenReturn(Optional.of(sampleInstitution));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            InstitutionResponse response = service.updateInstitution(1L, updateRequest);

            assertThat(response.name()).isEqualTo("Bank One Updated");
            assertThat(response.branchId()).isEqualTo("BR002");
            assertThat(response.isActive()).isFalse();
            assertThat(response.supportedNetworks()).containsExactly("SWIFT");
        }

        @Test
        void updateInstitution_nonExistentId_throwsNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateInstitution(999L, validRequest))
                    .isInstanceOf(InstitutionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Institution Tests (L-AC-005)")
    class DeleteInstitutionTests {

        @Test
        void deleteInstitution_softDelete_setsInactive() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleInstitution));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.deleteInstitution(1L);

            assertThat(sampleInstitution.getIsActive()).isFalse();
        }

        @Test
        void deleteInstitution_nonExistentId_throwsNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteInstitution(999L))
                    .isInstanceOf(InstitutionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Find By Institution ID Tests (L-AC-006)")
    class FindByInstitutionIdTests {

        @Test
        void findByInstitutionId_exists_returnsInstitution() {
            when(repository.findByInstitutionId("BANK01")).thenReturn(Optional.of(sampleInstitution));

            Optional<Institution> result = service.findByInstitutionId("BANK01");

            assertThat(result).isPresent();
            assertThat(result.get().getInstitutionId()).isEqualTo("BANK01");
        }

        @Test
        void findByInstitutionId_notFound_returnsEmpty() {
            when(repository.findByInstitutionId("UNKNOWN")).thenReturn(Optional.empty());

            Optional<Institution> result = service.findByInstitutionId("UNKNOWN");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validate Institution Tests (L-AC-007, AC-006)")
    class ValidateInstitutionTests {

        @Test
        void validateInstitution_valid_returnsSuccess() {
            when(repository.findByInstitutionId("BANK01")).thenReturn(Optional.of(sampleInstitution));

            ValidationResult result = service.validateInstitution("BANK01", "SWIFT");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        void validateInstitution_notFound_returnsFailed() {
            when(repository.findByInstitutionId("MISSING")).thenReturn(Optional.empty());

            ValidationResult result = service.validateInstitution("MISSING", "SWIFT");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).code()).isEqualTo(ErrorCode.MSG_010.getCode());
        }

        @Test
        void validateInstitution_inactive_returnsFailed() {
            Institution inactive = Institution.builder()
                    .institutionId("BANK01").isActive(false)
                    .supportedNetworks(List.of("SWIFT"))
                    .build();
            when(repository.findByInstitutionId("BANK01")).thenReturn(Optional.of(inactive));

            ValidationResult result = service.validateInstitution("BANK01", "SWIFT");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).code()).isEqualTo(ErrorCode.MSG_011.getCode());
        }

        @Test
        void validateInstitution_networkNotSupported_returnsFailed() {
            when(repository.findByInstitutionId("BANK01")).thenReturn(Optional.of(sampleInstitution));

            // sampleInstitution supports SWIFT and SEPA; request a network it does NOT support
            ValidationResult result = service.validateInstitution("BANK01", "TARGET2");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).code()).isEqualTo(ErrorCode.MSG_012.getCode());
        }

        @Test
        void validateInstitution_networkNotSupported_sepaOnly_returnsFailed() {
            Institution sepaOnly = Institution.builder()
                    .institutionId("BANK03").isActive(true)
                    .supportedNetworks(List.of("SEPA"))
                    .build();
            when(repository.findByInstitutionId("BANK03")).thenReturn(Optional.of(sepaOnly));

            ValidationResult result = service.validateInstitution("BANK03", "SWIFT");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).code()).isEqualTo(ErrorCode.MSG_012.getCode());
        }

        @Test
        void validateInstitution_nullNetworks_returnsNetworkFailed() {
            Institution noNetworks = Institution.builder()
                    .institutionId("BANK04").isActive(true)
                    .supportedNetworks(null)
                    .build();
            when(repository.findByInstitutionId("BANK04")).thenReturn(Optional.of(noNetworks));

            ValidationResult result = service.validateInstitution("BANK04", "SWIFT");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errors().get(0).code()).isEqualTo(ErrorCode.MSG_012.getCode());
        }
    }
}
