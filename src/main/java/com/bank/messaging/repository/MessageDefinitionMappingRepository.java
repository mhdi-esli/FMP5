package com.bank.messaging.repository;

import com.bank.messaging.entity.MessageDefinitionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Message Definition Mapping entities.
 * This is a stub for EPIC-02 - will be replaced when EPIC-02 is implemented.
 */
@Repository
public interface MessageDefinitionMappingRepository extends JpaRepository<MessageDefinitionMapping, Long> {

    /**
     * Find active message definition by message type and network.
     *
     * @param messageType the message type
     * @param network the network
     * @return the message definition if found
     */
    Optional<MessageDefinitionMapping> findByMessageTypeAndNetworkAndIsActiveTrue(String messageType, String network);

    /**
     * Find any message definition by message type and network.
     *
     * @param messageType the message type
     * @param network the network
     * @return the message definition if found
     */
    Optional<MessageDefinitionMapping> findByMessageTypeAndNetwork(String messageType, String network);
}
