-- Align the schema with Institution.supportedNetworks, which is mapped as an
-- ElementCollection. Preserve network data seeded by the original schema.
CREATE TABLE institution_supported_networks (
    institution_id BIGINT NOT NULL,
    network VARCHAR(20) NOT NULL,
    PRIMARY KEY (institution_id, network),
    CONSTRAINT fk_institution_supported_networks_institution
        FOREIGN KEY (institution_id) REFERENCES institutions (id) ON DELETE CASCADE
);

INSERT INTO institution_supported_networks (institution_id, network)
SELECT i.id, network
FROM institutions i
CROSS JOIN LATERAL unnest(i.supported_networks) AS network
WHERE i.supported_networks IS NOT NULL;

ALTER TABLE institutions DROP COLUMN supported_networks;
