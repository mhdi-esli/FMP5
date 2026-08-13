package com.bank.messaging.service;

import com.bank.messaging.dto.MessageDefinitionRequest;
import com.bank.messaging.dto.MessageDefinitionResponse;
import com.bank.messaging.entity.MessageDefinitionMapping;
import com.bank.messaging.exception.DefinitionNotFoundException;
import com.bank.messaging.exception.DuplicateDefinitionException;
import com.bank.messaging.repository.MessageDefinitionMappingRepository;
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
 * Integration tests for MessageDefinitionService.
 * Tests CRUD operations and business logic with full Spring context.
 */
@SpringBootTest
@Transactional
class MessageDefinitionServiceIntegrationTest {

    @Autowired
    private MessageDefinitionService service;

    @Autowired
    private MessageDefinitionMappingRepository repository;

    private MessageDefinitionRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new MessageDefinitionRequest(
            "MT200", "SWIFT", 1,
            "{\"field1\": \"value1\"}",
            "{\"rule1\": \"active\"}",
            true
        );
    }

    @Nested
    @DisplayName("Create Definition Tests")
    class CreateDefinitionTests {

        @Test
        @DisplayName("createDefinition_validRequest_returnsCreated")
        void createDefinition_validRequest_returnsCreated() {
            // Given
            MessageDefinitionRequest request = validRequest;

            // When
            MessageDefinitionResponse response = service.createDefinition(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isNotNull();
            assertThat(response.messageType()).isEqualTo("MT200");
            assertThat(response.network()).isEqualTo("SWIFT");
            assertThat(response.version()).isEqualTo(1);
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("createDefinition_duplicate_throwsException")
        void createDefinition_duplicate_throwsException() {
            // Given
            service.createDefinition(validRequest);

            // When & Then
            assertThatThrownBy(() -> service.createDefinition(validRequest))
                .isInstanceOf(DuplicateDefinitionException.class)
                .hasMessageContaining("MT200")
                .hasMessageContaining("SWIFT");
        }
    }

    @Nested
    @DisplayName("Get Definition Tests")
    class GetDefinitionTests {

        @Test
        @DisplayName("getDefinition_existing_returnsDefinition")
        void getDefinition_existing_returnsDefinition() {
            // Given
            MessageDefinitionResponse created = service.createDefinition(validRequest);

            // When
            MessageDefinitionResponse response = service.getDefinition(created.id());

            // Then
            assertThat(response.id()).isEqualTo(created.id());
            assertThat(response.messageType()).isEqualTo("MT200");
        }

        @Test
        @DisplayName("getDefinition_nonExistent_throwsException")
        void getDefinition_nonExistent_throwsException() {
            // Given
            long nonExistentId = 9999L;

            // When & Then
            assertThatThrownBy(() -> service.getDefinition(nonExistentId))
                .isInstanceOf(DefinitionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("List Definitions Tests")
    class ListDefinitionsTests {

        @Test
        @DisplayName("listDefinitions_noFilter_returnsAll")
        void listDefinitions_noFilter_returnsAll() {
            // Given
            service.createDefinition(new MessageDefinitionRequest(
                "MT200", "SWIFT", 1, null, null, true));
            service.createDefinition(new MessageDefinitionRequest(
                "MT202", "SEPA", 1, null, null, true));

            // When
            List<MessageDefinitionResponse> definitions = service.listDefinitions(null, null, null);

            // Then
            assertThat(definitions).hasSize(2);
        }

        @Test
        @DisplayName("listDefinitions_filteredByMessageType_returnsFiltered")
        void listDefinitions_filteredByMessageType_returnsFiltered() {
            // Given
            service.createDefinition(new MessageDefinitionRequest(
                "MT200", "SWIFT", 1, null, null, true));
            service.createDefinition(new MessageDefinitionRequest(
                "MT202", "SWIFT", 1, null, null, true));

            // When
            List<MessageDefinitionResponse> definitions = service.listDefinitions("MT200", null, null);

            // Then
            assertThat(definitions).hasSize(1);
            assertThat(definitions.get(0).messageType()).isEqualTo("MT200");
        }

        @Test
        @DisplayName("listDefinitions_filteredByNetwork_returnsFiltered")
        void listDefinitions_filteredByNetwork_returnsFiltered() {
            // Given
            service.createDefinition(new MessageDefinitionRequest(
                "MT200", "SWIFT", 1, null, null, true));
            service.createDefinition(new MessageDefinitionRequest(
                "MT202", "SEPA", 1, null, null, true));

            // When
            List<MessageDefinitionResponse> definitions = service.listDefinitions(null, "SWIFT", null);

            // Then
            assertThat(definitions).hasSize(1);
            assertThat(definitions.get(0).network()).isEqualTo("SWIFT");
        }

        @Test
        @DisplayName("listDefinitions_filteredByActive_returnsFiltered")
        void listDefinitions_filteredByActive_returnsFiltered() {
            // Given
            service.createDefinition(new MessageDefinitionRequest(
                "MT200", "SWIFT", 1, null, null, true));
            service.createDefinition(new MessageDefinitionRequest(
                "MT202", "SWIFT", 1, null, null, false));

            // When
            List<MessageDefinitionResponse> definitions = service.listDefinitions(null, null, true);

            // Then
            assertThat(definitions).hasSize(1);
            assertThat(definitions.get(0).isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Update Definition Tests")
    class UpdateDefinitionTests {

        @Test
        @DisplayName("updateDefinition_existing_returnsUpdated")
        void updateDefinition_existing_returnsUpdated() {
            // Given
            MessageDefinitionResponse created = service.createDefinition(validRequest);

            // When
            MessageDefinitionRequest updateRequest = new MessageDefinitionRequest(
                "MT200", "SWIFT", 2, null, null, false);
            MessageDefinitionResponse response = service.updateDefinition(created.id(), updateRequest);

            // Then
            assertThat(response.id()).isEqualTo(created.id());
            assertThat(response.version()).isEqualTo(2);
            assertThat(response.isActive()).isFalse();
        }

        @Test
        @DisplayName("updateDefinition_nonExistent_throwsException")
        void updateDefinition_nonExistent_throwsException() {
            // Given
            long nonExistentId = 9999L;
            MessageDefinitionRequest request = new MessageDefinitionRequest(
                "MT200", "SWIFT", 1, null, null, true);

            // When & Then
            assertThatThrownBy(() -> service.updateDefinition(nonExistentId, request))
                .isInstanceOf(DefinitionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Definition Tests")
    class DeleteDefinitionTests {

        @Test
        @DisplayName("deleteDefinition_existing_setsInactive")
        void deleteDefinition_existing_setsInactive() {
            // Given
            MessageDefinitionResponse created = service.createDefinition(validRequest);

            // When
            service.deleteDefinition(created.id());

            // Then
            Optional<MessageDefinitionMapping> byId = repository.findById(created.id());
            assertThat(byId).isPresent();
            assertThat(byId.get().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("deleteDefinition_nonExistent_throwsException")
        void deleteDefinition_nonExistent_throwsException() {
            // Given
            long nonExistentId = 9999L;

            // When & Then
            assertThatThrownBy(() -> service.deleteDefinition(nonExistentId))
                .isInstanceOf(DefinitionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Find Active Definition Tests")
    class FindActiveDefinitionTests {

        @Test
        @DisplayName("findActiveDefinition_active_returnsDefinition")
        void findActiveDefinition_active_returnsDefinition() {
            // Given
            service.createDefinition(new MessageDefinitionRequest(
                "MT200", "SWIFT", 1, null, null, true));

            // When
            Optional<MessageDefinitionMapping> result = service.findActiveDefinition("MT200", "SWIFT");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getMessageType()).isEqualTo("MT200");
        }

        @Test
        @DisplayName("findActiveDefinition_inactive_returnsEmpty")
        void findActiveDefinition_inactive_returnsEmpty() {
            // Given
            service.createDefinition(new MessageDefinitionRequest(
                "MT200", "SWIFT", 1, null, null, false));

            // When
            Optional<MessageDefinitionMapping> result = service.findActiveDefinition("MT200", "SWIFT");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("findActiveDefinition_notFound_returnsEmpty")
        void findActiveDefinition_notFound_returnsEmpty() {
            // When
            Optional<MessageDefinitionMapping> result = service.findActiveDefinition("UNKNOWN", "SWIFT");

            // Then
            assertThat(result).isEmpty();
        }
    }
}
