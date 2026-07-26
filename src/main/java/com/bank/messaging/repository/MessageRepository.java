package com.bank.messaging.repository;

import com.bank.messaging.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Message entities.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find a message by its unique message ID.
     *
     * @param messageId the message ID
     * @return the message if found
     */
    Optional<Message> findByMessageId(String messageId);

    /**
     * Check if a message exists by message ID.
     *
     * @param messageId the message ID
     * @return true if exists
     */
    boolean existsByMessageId(String messageId);
}
