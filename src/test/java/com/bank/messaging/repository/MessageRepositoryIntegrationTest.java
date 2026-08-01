package com.bank.messaging.repository;

import com.bank.messaging.entity.Message;
import com.bank.messaging.enums.MessageStatus;
import com.bank.messaging.enums.Network;
import com.bank.messaging.enums.ValidationResultEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MessageRepository.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MessageRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    @DisplayName("save_validMessage_persistsToDatabase")
    void save_validMessage_persistsToDatabase() {
        // Given
        Message message = Message.builder()
            .messageId("MSG-20260726-000001")
            .messageType("200")
            .network(Network.SWIFT)
            .status(MessageStatus.DRAFT)
            .requestReference("REQ-10001")
            .transactionReference("TRX-458796")
            .valueDate(LocalDate.of(2026, 7, 25))
            .currency("USD")
            .amount(BigDecimal.valueOf(25000))
            .senderInstitutionId("BANK01")
            .receiverInstitutionId("BANK02")
            .validationResult(ValidationResultEnum.SUCCESS)
            .build();

        // When
        Message saved = messageRepository.save(message);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMessageId()).isEqualTo("MSG-20260726-000001");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByMessageId_existingId_returnsMessage")
    void findByMessageId_existingId_returnsMessage() {
        // Given
        Message message = Message.builder()
            .messageId("MSG-20260726-000002")
            .messageType("200")
            .network(Network.SWIFT)
            .status(MessageStatus.DRAFT)
            .validationResult(ValidationResultEnum.SUCCESS)
            .build();

        entityManager.persistAndFlush(message);

        // When
        var found = messageRepository.findByMessageId("MSG-20260726-000002");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getMessageId()).isEqualTo("MSG-20260726-000002");
    }

    @Test
    @DisplayName("save_validationErrors_storedAsJsonb")
    void save_validationErrors_storedAsJsonb() {
        // Given
        String errorsJson = "[{\"code\":\"MSG-001\",\"field\":\"amount\",\"message\":\"Invalid amount\"}]";

        Message message = Message.builder()
            .messageId("MSG-20260726-000003")
            .messageType("200")
            .network(Network.SWIFT)
            .status(MessageStatus.VALIDATION_FAILED)
            .validationResult(ValidationResultEnum.FAILED)
            .validationErrors(errorsJson)
            .build();

        // When
        Message saved = messageRepository.save(message);

        // Then
        assertThat(saved.getValidationErrors()).isNotNull();
        assertThat(saved.getValidationErrors()).contains("MSG-001");
    }
}
