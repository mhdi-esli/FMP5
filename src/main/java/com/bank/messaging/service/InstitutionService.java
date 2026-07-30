package com.bank.messaging.service;

import com.bank.messaging.dto.InstitutionRequest;
import com.bank.messaging.dto.InstitutionResponse;
import com.bank.messaging.dto.ValidationError;
import com.bank.messaging.entity.Institution;
import com.bank.messaging.enums.ErrorCode;
import com.bank.messaging.exception.DuplicateInstitutionException;
import com.bank.messaging.exception.InstitutionNotFoundException;
import com.bank.messaging.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing financial institutions (EPIC-03).
 * Provides CRUD operations, a cached business-ID lookup, and institution
 * validation used by EPIC-01's message creation flow.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository repository;

    /**
     * Create a new institution.
     *
     * @param request the institution details
     * @return the created institution
     * @throws DuplicateInstitutionException if an institution with the same institutionId already exists
     */
    @Transactional
    @CacheEvict(value = "institutions", allEntries = true)
    public InstitutionResponse createInstitution(InstitutionRequest request) {
        log.info("Creating institution: {}", request.institutionId());

        Optional<Institution> existing = repository.findByInstitutionId(request.institutionId());
        if (existing.isPresent()) {
            throw new DuplicateInstitutionException(request.institutionId());
        }

        Institution entity = Institution.builder()
                .institutionId(request.institutionId())
                .bic(request.bic())
                .name(request.name())
                .branchId(request.branchId())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .supportedNetworks(request.supportedNetworks())
                .build();

        Institution saved = repository.save(entity);
        log.info("Created institution with id: {}", saved.getId());
        return InstitutionResponse.fromEntity(saved);
    }

    /**
     * Get an institution by its database ID.
     *
     * @param id the database ID
     * @return the institution
     * @throws InstitutionNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public InstitutionResponse getInstitution(Long id) {
        Institution entity = repository.findById(id)
                .orElseThrow(() -> new InstitutionNotFoundException(String.valueOf(id)));
        return InstitutionResponse.fromEntity(entity);
    }

    /**
     * List institutions with optional filters.
     *
     * @param name     optional filter by name (case-insensitive contains)
     * @param network  optional filter by supported network
     * @param isActive optional filter by active status
     * @return filtered list of institutions
     */
    @Transactional(readOnly = true)
    public List<InstitutionResponse> listInstitutions(String name, String network, Boolean isActive) {
        List<Institution> entities;

        if (network != null) {
            entities = repository.findBySupportedNetwork(network);
        } else if (name != null) {
            entities = repository.findByNameContainingIgnoreCase(name);
        } else if (isActive != null && isActive) {
            entities = repository.findByIsActiveTrue();
        } else if (isActive != null) {
            entities = repository.findByIsActiveFalse();
        } else {
            entities = repository.findAll();
        }

        return entities.stream()
                .map(InstitutionResponse::fromEntity)
                .toList();
    }

    /**
     * Update an existing institution.
     *
     * @param id      the database ID
     * @param request the updated fields
     * @return the updated institution
     * @throws InstitutionNotFoundException if not found
     */
    @Transactional
    @CacheEvict(value = "institutions", allEntries = true)
    public InstitutionResponse updateInstitution(Long id, InstitutionRequest request) {
        log.info("Updating institution id: {}", id);

        Institution entity = repository.findById(id)
                .orElseThrow(() -> new InstitutionNotFoundException(String.valueOf(id)));

        if (request.institutionId() != null) {
            entity.setInstitutionId(request.institutionId());
        }
        if (request.bic() != null) {
            entity.setBic(request.bic());
        }
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.branchId() != null) {
            entity.setBranchId(request.branchId());
        }
        if (request.isActive() != null) {
            entity.setIsActive(request.isActive());
        }
        if (request.supportedNetworks() != null) {
            entity.setSupportedNetworks(request.supportedNetworks());
        }

        entity.setUpdatedAt(LocalDateTime.now());
        Institution saved = repository.save(entity);
        return InstitutionResponse.fromEntity(saved);
    }

    /**
     * Soft-delete an institution by setting isActive to false.
     *
     * @param id the database ID
     * @throws InstitutionNotFoundException if not found
     */
    @Transactional
    @CacheEvict(value = "institutions", allEntries = true)
    public void deleteInstitution(Long id) {
        log.info("Soft-deleting institution id: {}", id);

        Institution entity = repository.findById(id)
                .orElseThrow(() -> new InstitutionNotFoundException(String.valueOf(id)));

        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
    }

    /**
     * Find an institution by its business identifier (institutionId).
     * Used by EPIC-01 message creation. Cached with short TTL.
     *
     * @param institutionId the business institution ID
     * @return the institution, if found
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "institutions", key = "#institutionId", unless = "#result == null")
    public Optional<Institution> findByInstitutionId(String institutionId) {
        return repository.findByInstitutionId(institutionId);
    }

    /**
     * Validate that a sender institution exists, is active, and supports the
     * requested network. Used by EPIC-01's MessageCreationService.
     *
     * @param institutionId the business institution ID
     * @param network       the network the institution must support
     * @return success if valid, otherwise failed with the appropriate MSG-01x error
     */
    @Transactional(readOnly = true)
    public ValidationResult validateInstitution(String institutionId, String network) {
        Optional<Institution> institutionOpt = findByInstitutionId(institutionId);

        if (institutionOpt.isEmpty()) {
            log.warn("Institution not found: {}", institutionId);
            return ValidationResult.failed(new ValidationError(
                    ErrorCode.MSG_010.getCode(),
                    "senderInstitutionIdentifier",
                    ErrorCode.MSG_010.getPersianMessage()
            ));
        }

        Institution institution = institutionOpt.get();

        if (!Boolean.TRUE.equals(institution.getIsActive())) {
            log.warn("Institution inactive: {}", institutionId);
            return ValidationResult.failed(new ValidationError(
                    ErrorCode.MSG_011.getCode(),
                    "senderInstitutionIdentifier",
                    ErrorCode.MSG_011.getPersianMessage()
            ));
        }

        List<String> supported = institution.getSupportedNetworks();
        if (supported == null || !supported.contains(network)) {
            log.warn("Institution {} does not support network: {}", institutionId, network);
            return ValidationResult.failed(new ValidationError(
                    ErrorCode.MSG_012.getCode(),
                    "senderInstitutionIdentifier",
                    ErrorCode.MSG_012.getPersianMessage()
            ));
        }

        return ValidationResult.success();
    }
}
