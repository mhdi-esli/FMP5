package com.bank.messaging.repository;

import com.bank.messaging.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Find the maximum sequence number for messages created today.
     * Used to initialize the sequence counter on application startup.
     *
     * @return the maximum sequence number, or 0 if no messages exist today
     */
    @Query("SELECT COALESCE(MAX(SUBSTRING(m.messageId, 13, 6))::integer, 0) FROM Message m WHERE m.messageId LIKE :prefix%")
    int findMaxSequenceForToday(@Param("prefix") String prefix);
}
