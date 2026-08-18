package com.bank.messaging.controller;

import com.bank.messaging.dto.InstitutionRequest;
import com.bank.messaging.dto.InstitutionResponse;
import com.bank.messaging.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing financial institutions (EPIC-03).
 */
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@Tag(name = "Institutions", description = "CRUD operations for financial institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    /**
     * Create a new institution.
     */
    @PostMapping
    @Operation(summary = "Create institution", description = "Creates a new financial institution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Institution created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Duplicate institution (institutionId already exists)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<InstitutionResponse> createInstitution(
            @Valid @RequestBody InstitutionRequest request) {
        InstitutionResponse response = institutionService.createInstitution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get an institution by its database ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get institution by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Institution found"),
        @ApiResponse(responseCode = "404", description = "Institution not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<InstitutionResponse> getInstitution(@PathVariable Long id) {
        InstitutionResponse response = institutionService.getInstitution(id);
        return ResponseEntity.ok(response);
    }

    /**
     * List institutions with optional filters.
     */
    @GetMapping
    @Operation(summary = "List institutions", description = "List institutions with optional filters: name, network, isActive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of institutions"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<InstitutionResponse>> listInstitutions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) Boolean isActive) {
        List<InstitutionResponse> responses = institutionService.listInstitutions(name, network, isActive);
        return ResponseEntity.ok(responses);
    }

    /**
     * Update an existing institution.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update institution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Institution updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Institution not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<InstitutionResponse> updateInstitution(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionRequest request) {
        InstitutionResponse response = institutionService.updateInstitution(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-delete an institution (deactivates it).
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete institution", description = "Soft-delete (deactivates) a financial institution")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Institution deactivated"),
        @ApiResponse(responseCode = "404", description = "Institution not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteInstitution(@PathVariable Long id) {
        institutionService.deleteInstitution(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lookup an institution by its business identifier (institutionId).
     * Used internally by EPIC-01 for message creation validation.
     */
    @GetMapping("/lookup")
    @Operation(summary = "Lookup institution", description = "Find an institution by its business institutionId (used by EPIC-01)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Institution found"),
        @ApiResponse(responseCode = "404", description = "Institution not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<InstitutionResponse> lookupInstitution(
            @RequestParam String institutionId) {
        return institutionService.findByInstitutionId(institutionId)
                .map(InstitutionResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
