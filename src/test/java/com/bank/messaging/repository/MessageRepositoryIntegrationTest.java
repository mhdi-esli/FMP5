package com.bank.messaging.repository;

import com.bank.messaging.entity.Message;
import com.bank.messaging.enums.MessageStatus;
import com.bank.messaging.enums.Network;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MessageRepository.
 */
@DataJpaTest
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
                .validationResult(null)
                .validationErrors(null)
                .createdAt(null)
                .createdBy(null)
                .build();
        // When
        Message saved = messageRepository.save(message);
        // Then
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("save_validationErrors_storedAsJsonb")
    void save_validationErrors_storedAsJsonb() {
        Message message = Message.builder()
                .messageId("MSG-20260726-000002")
                .messageType("200")
                .network(Network.SWIFT)
                .status(MessageStatus.DRAFT)
                .validationResult(null)
                .validationErrors("[{\"code\":\"MSG-001\",\"field\":\"amount\",\"message\":\"Invalid amount\"}]")
                .build();
        Message saved = entityManager.persistFlushFind(message);
        assertThat(saved.getValidationErrors()).isNotNull();
    }
}
