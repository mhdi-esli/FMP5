package com.bank.messaging.service;

import com.bank.messaging.dto.MessageDefinitionRequest;
import com.bank.messaging.dto.MessageDefinitionResponse;
import com.bank.messaging.entity.MessageDefinitionMapping;
import com.bank.messaging.exception.DefinitionNotFoundException;
import com.bank.messaging.exception.DuplicateDefinitionException;
import com.bank.messaging.exception.InvalidJsonException;
import com.bank.messaging.repository.MessageDefinitionMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageDefinitionServiceTest {

    @Mock
    private MessageDefinitionMappingRepository repository;

    private MessageDefinitionService service;

    private MessageDefinitionMapping sampleMapping;
    private MessageDefinitionRequest validRequest;

    @BeforeEach
    void setUp() {
        service = new MessageDefinitionService(repository);
        sampleMapping = MessageDefinitionMapping.builder()
                .id(1L)
                .messageType("MT200")
                .network("SWIFT")
                .version(1)
                .fieldMappings("{\"field\": \"value\"}")
                .validationRules("{\"rule\": \"active\"}")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validRequest = new MessageDefinitionRequest("MT200", "SWIFT", 1,
                "{\"field\": \"value\"}", "{\"rule\": \"active\"}", true);
    }

    @Nested
    @DisplayName("Create Definition Tests")
    class CreateDefinitionTests {

        @Test
        void createDefinition_success_returnsCreatedDefinition() {
            when(repository.findByMessageTypeAndNetwork("MT200", "SWIFT")).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(sampleMapping);

            MessageDefinitionResponse response = service.createDefinition(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.messageType()).isEqualTo("MT200");
            assertThat(response.network()).isEqualTo("SWIFT");
            assertThat(response.version()).isEqualTo(1);
            assertThat(response.isActive()).isTrue();
        }

        @Test
        void createDefinition_duplicateTypeNetworkVersion_throwsException() {
            when(repository.findByMessageTypeAndNetwork("MT200", "SWIFT"))
                    .thenReturn(Optional.of(sampleMapping));

            assertThatThrownBy(() -> service.createDefinition(validRequest))
                    .isInstanceOf(DuplicateDefinitionException.class)
                    .hasMessageContaining("MT200");
        }

        @Test
        void createDefinition_withFieldMappings_storesJsonb() {
            when(repository.findByMessageTypeAndNetwork("MT200", "SWIFT")).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(sampleMapping);

            MessageDefinitionResponse response = service.createDefinition(validRequest);

            assertThat(response.fieldMappings()).isEqualTo("{\"field\": \"value\"}");
        }

        @Test
        void createDefinition_withoutActive_defaultsToTrue() {
            MessageDefinitionRequest requestNoActive = new MessageDefinitionRequest(
                    "MT202", "SEPA", 1, null, null, null);

            when(repository.findByMessageTypeAndNetwork("MT202", "SEPA")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(invocation -> {
                MessageDefinitionMapping saved = invocation.getArgument(0);
                saved.setId(2L);
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });

            MessageDefinitionResponse response = service.createDefinition(requestNoActive);

            assertThat(response.isActive()).isTrue();
        }

        @Test
        void createDefinition_invalidFieldMappingsJson_throwsException() {
            MessageDefinitionRequest invalidRequest = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 1,
                    "{broken json}", null, true);

            assertThatThrownBy(() -> service.createDefinition(invalidRequest))
                    .isInstanceOf(InvalidJsonException.class)
                    .hasMessageContaining("fieldMappings");
        }

        @Test
        void createDefinition_invalidValidationRulesJson_throwsException() {
            MessageDefinitionRequest invalidRequest = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 1,
                    null, "{bad json", true);

            assertThatThrownBy(() -> service.createDefinition(invalidRequest))
                    .isInstanceOf(InvalidJsonException.class)
                    .hasMessageContaining("validationRules");
        }

        @Test
        void createDefinition_nullJsonFields_shouldNotValidate() {
            MessageDefinitionRequest nullJsonRequest = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 1, null, null, true);

            when(repository.findByMessageTypeAndNetwork("MT200", "SWIFT")).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(sampleMapping);

            MessageDefinitionResponse response = service.createDefinition(nullJsonRequest);

            assertThat(response).isNotNull();
            assertThat(response.messageType()).isEqualTo("MT200");
        }
    }

    @Nested
    @DisplayName("Get Definition Tests")
    class GetDefinitionTests {

        @Test
        void getDefinition_existingId_returnsDefinition() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleMapping));

            MessageDefinitionResponse response = service.getDefinition(1L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
        }

        @Test
        void getDefinition_nonExistentId_throwsNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDefinition(999L))
                    .isInstanceOf(DefinitionNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("List Definitions Tests")
    class ListDefinitionsTests {

        @Test
        void listDefinitions_noFilter_returnsAll() {
            when(repository.findAll()).thenReturn(List.of(sampleMapping));

            List<MessageDefinitionResponse> responses = service.listDefinitions(null, null, null);

            assertThat(responses).hasSize(1);
        }

        @Test
        void listDefinitions_filterByType_returnsFiltered() {
            when(repository.findByMessageType("MT200")).thenReturn(List.of(sampleMapping));

            List<MessageDefinitionResponse> responses = service.listDefinitions("MT200", null, null);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).messageType()).isEqualTo("MT200");
        }

        @Test
        void listDefinitions_filterByNetwork_returnsFiltered() {
            when(repository.findByNetwork("SWIFT")).thenReturn(List.of(sampleMapping));

            List<MessageDefinitionResponse> responses = service.listDefinitions(null, "SWIFT", null);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).network()).isEqualTo("SWIFT");
        }

        @Test
        void listDefinitions_filterByActive_returnsFiltered() {
            when(repository.findByIsActiveTrue()).thenReturn(List.of(sampleMapping));

            List<MessageDefinitionResponse> responses = service.listDefinitions(null, null, true);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Update Definition Tests")
    class UpdateDefinitionTests {

        @Test
        void updateDefinition_success_updatesFields() {
            MessageDefinitionRequest updateRequest = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 2,
                    "{\"new\": \"mapping\"}", "{\"new\": \"rule\"}", true);

            when(repository.findById(1L)).thenReturn(Optional.of(sampleMapping));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            MessageDefinitionResponse response = service.updateDefinition(1L, updateRequest);

            assertThat(response.version()).isEqualTo(2);
            assertThat(response.fieldMappings()).isEqualTo("{\"new\": \"mapping\"}");
            assertThat(response.validationRules()).isEqualTo("{\"new\": \"rule\"}");
        }

        @Test
        void updateDefinition_nonExistentId_throwsNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateDefinition(999L, validRequest))
                    .isInstanceOf(DefinitionNotFoundException.class);
        }

        @Test
        void updateDefinition_deactivates_keepsHistory() {
            MessageDefinitionRequest deactivateRequest = new MessageDefinitionRequest(
                    "MT200", "SWIFT", 1, null, null, false);

            when(repository.findById(1L)).thenReturn(Optional.of(sampleMapping));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            MessageDefinitionResponse response = service.updateDefinition(1L, deactivateRequest);

            assertThat(response.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Delete Definition Tests")
    class DeleteDefinitionTests {

        @Test
        void deleteDefinition_softDelete_setsInactive() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleMapping));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.deleteDefinition(1L);

            assertThat(sampleMapping.getIsActive()).isFalse();
        }

        @Test
        void deleteDefinition_nonExistentId_throwsNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteDefinition(999L))
                    .isInstanceOf(DefinitionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Find Active Definition Tests")
    class FindActiveDefinitionTests {

        @Test
        void findActiveDefinition_exists_returnsDefinition() {
            when(repository.findByMessageTypeAndNetworkAndIsActiveTrue("MT200", "SWIFT"))
                    .thenReturn(Optional.of(sampleMapping));

            Optional<MessageDefinitionMapping> result = service.findActiveDefinition("MT200", "SWIFT");

            assertThat(result).isPresent();
            assertThat(result.get().getMessageType()).isEqualTo("MT200");
        }

        @Test
        void findActiveDefinition_notFound_returnsEmpty() {
            when(repository.findByMessageTypeAndNetworkAndIsActiveTrue("UNKNOWN", "SWIFT"))
                    .thenReturn(Optional.empty());

            Optional<MessageDefinitionMapping> result = service.findActiveDefinition("UNKNOWN", "SWIFT");

            assertThat(result).isEmpty();
        }

        @Test
        void findActiveDefinition_inactive_returnsEmpty() {
            when(repository.findByMessageTypeAndNetworkAndIsActiveTrue("MT200", "SWIFT"))
                    .thenReturn(Optional.empty());

            Optional<MessageDefinitionMapping> result = service.findActiveDefinition("MT200", "SWIFT");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find Any Definition Tests")
    class FindDefinitionTests {

        @Test
        void findDefinition_exists_returnsDefinition() {
            when(repository.findByMessageTypeAndNetwork("MT200", "SWIFT"))
                    .thenReturn(Optional.of(sampleMapping));

            Optional<MessageDefinitionMapping> result = service.findDefinition("MT200", "SWIFT");

            assertThat(result).isPresent();
            assertThat(result.get().getMessageType()).isEqualTo("MT200");
        }

        @Test
        void findDefinition_notFound_returnsEmpty() {
            when(repository.findByMessageTypeAndNetwork("UNKNOWN", "SWIFT"))
                    .thenReturn(Optional.empty());

            Optional<MessageDefinitionMapping> result = service.findDefinition("UNKNOWN", "SWIFT");

            assertThat(result).isEmpty();
        }
    }
}
