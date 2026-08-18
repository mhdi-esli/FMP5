-- Messages table
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(50) UNIQUE NOT NULL,
    message_type VARCHAR(10) NOT NULL,
    network VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    request_reference VARCHAR(100),
    transaction_reference VARCHAR(100),
    related_reference VARCHAR(100),
    value_date DATE,
    currency VARCHAR(3),
    amount DECIMAL(20, 4),
    sender_institution_id VARCHAR(50),
    sender_bic VARCHAR(11),
    sender_branch_id VARCHAR(50),
    receiver_institution_id VARCHAR(50),
    receiver_bic VARCHAR(11),
    receiver_branch_id VARCHAR(50),
    charge_type VARCHAR(3),
    instruction_code VARCHAR(10),
    narrative TEXT,
    additional_information TEXT,
    validation_result VARCHAR(20),
    validation_errors JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- Create index on message_id for fast lookups
CREATE INDEX idx_messages_message_id ON messages(message_id);
CREATE INDEX idx_messages_status ON messages(status);
CREATE INDEX idx_messages_created_at ON messages(created_at);

-- Institutions table (stub for EPIC-03)
CREATE TABLE institutions (
    id BIGSERIAL PRIMARY KEY,
    institution_id VARCHAR(50) UNIQUE NOT NULL,
    bic VARCHAR(11),
    name VARCHAR(200),
    branch_id VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    supported_networks TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Message Definition mappings (stub for EPIC-02)
CREATE TABLE message_definition_mappings (
    id BIGSERIAL PRIMARY KEY,
    message_type VARCHAR(10) NOT NULL,
    network VARCHAR(20) NOT NULL,
    version INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    field_mappings JSONB,
    validation_rules JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(message_type, network, version)
);

-- Insert stub data for EPIC-02 (Message Definition)
INSERT INTO message_definition_mappings (message_type, network, version, is_active, created_at)
VALUES ('200', 'SWIFT', 1, true, CURRENT_TIMESTAMP);

INSERT INTO message_definition_mappings (message_type, network, version, is_active, created_at)
VALUES ('200', 'SEPA', 1, true, CURRENT_TIMESTAMP);

-- Insert stub data for EPIC-03 (Institutions)
INSERT INTO institutions (institution_id, bic, name, is_active, supported_networks, created_at)
VALUES ('BANK01', 'BANK01XXX', 'Bank One', true, ARRAY['SWIFT', 'SEPA'], CURRENT_TIMESTAMP);

INSERT INTO institutions (institution_id, bic, name, is_active, supported_networks, created_at)
VALUES ('BANK02', 'BANK02XXX', 'Bank Two', true, ARRAY['SWIFT', 'SEPA'], CURRENT_TIMESTAMP);
