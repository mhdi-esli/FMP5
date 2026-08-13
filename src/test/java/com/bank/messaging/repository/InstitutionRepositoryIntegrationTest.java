package com.bank.messaging.repository;

import com.bank.messaging.entity.Institution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class InstitutionRepositoryIntegrationTest {

    @Autowired
    private InstitutionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void saveInstitution_withNetworks_persistsAndRetrievesNetworks() {
        Institution institution = institution("BANK01", true, List.of("SWIFT", "SEPA"));
        Institution saved = repository.saveAndFlush(institution);
        Institution retrieved = repository.findByInstitutionId("BANK01").orElseThrow();
        assertThat(retrieved.getSupportedNetworks()).containsExactly("SWIFT", "SEPA");
    }

    @Test
    void findBySupportedNetwork_returnsActiveInstitutionsOnly() {
        repository.saveAndFlush(institution("BANK01", true, List.of("SWIFT")));
        repository.saveAndFlush(institution("BANK02", false, List.of("SWIFT")));
        List<Institution> institutions = repository.findBySupportedNetwork("SWIFT");
        assertThat(institutions).hasSize(1);
        assertThat(institutions.get(0).getInstitutionId()).isEqualTo("BANK01");
    }

    @Test
    void duplicateInstitutionId_violatesUniqueConstraint() {
        repository.saveAndFlush(institution("BANK01", true, List.of("SWIFT")));
        assertThatThrownBy(() -> repository.saveAndFlush(institution("BANK01", true, List.of("SEPA"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Institution institution(String institutionId, boolean isActive, List<String> supportedNetworks) {
        return Institution.builder()
                .institutionId(institutionId)
                .name(institutionId + " Bank")
                .isActive(isActive)
                .supportedNetworks(supportedNetworks)
                .build();
    }
}
