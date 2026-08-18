package com.bank.messaging.repository;

import com.bank.messaging.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Institution entities.
 * This is a stub for EPIC-03 - will be replaced when EPIC-03 is implemented.
 */
@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    /**
     * Find an institution by its unique identifier.
     *
     * @param institutionId the institution ID
     * @return the institution if found
     */
    Optional<Institution> findByInstitutionId(String institutionId);

    /**
     * Find all active institutions.
     *
     * @return list of active institutions
     */
    List<Institution> findByIsActiveTrue();

    /**
     * Find all inactive institutions.
     *
     * @return list of inactive institutions
     */
    List<Institution> findByIsActiveFalse();

    /**
     * Find institutions whose name contains the given fragment (case-insensitive).
     *
     * @param name the name fragment to match
     * @return list of matching institutions
     */
    List<Institution> findByNameContainingIgnoreCase(String name);

    /**
     * Find institutions supporting a specific network.
     *
     * @param network the network name
     * @return list of institutions supporting the network
     */
    @org.springframework.data.jpa.repository.Query("SELECT i FROM Institution i WHERE :network MEMBER OF i.supportedNetworks AND i.isActive = true")
    List<Institution> findBySupportedNetwork(String network);
}
