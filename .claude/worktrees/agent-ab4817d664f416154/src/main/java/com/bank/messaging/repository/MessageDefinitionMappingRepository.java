package com.bank.messaging.repository;

import com.bank.messaging.entity.MessageDefinitionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Message Definition Mapping entities.
 */
@Repository
public interface MessageDefinitionMappingRepository extends JpaRepository<MessageDefinitionMapping, Long> {

    /**
     * Find active message definition by message type and network.
     */
    Optional<MessageDefinitionMapping> findByMessageTypeAndNetworkAndIsActiveTrue(String messageType, String network);

    /**
     * Find any message definition by message type and network.
     */
    Optional<MessageDefinitionMapping> findByMessageTypeAndNetwork(String messageType, String network);

    /**
     * Find all definitions by message type.
     */
    List<MessageDefinitionMapping> findByMessageType(String messageType);

    /**
     * Find all definitions by network.
     */
    List<MessageDefinitionMapping> findByNetwork(String network);

    /**
     * Find all definitions by active status.
     */
    List<MessageDefinitionMapping> findByIsActiveTrue();

    /**
     * Find all definitions by message type and network (list variant).
     */
    List<MessageDefinitionMapping> findAllByMessageTypeAndNetwork(String messageType, String network);
}
