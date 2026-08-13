-- Migration script to create tables for integration tests
-- This script creates tables in PostgreSQL using DO language for conditional execution

-- Check if the database exists and create tables only if they don't exist
DO $$
BEGIN
    -- Create institutions table
    EXECUTE '''
    CREATE TABLE IF NOT EXISTS institutions (
        institution_id VARCHAR(20) PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        is_active BOOLEAN NOT NULL,
        supported_networks JSON NOT NULL
    )
    ''';

    -- Create messages table
    EXECUTE '''
    CREATE TABLE IF NOT EXISTS messages (
        id BIGSERIAL PRIMARY KEY,
        message_id VARCHAR(50) NOT NULL UNIQUE,
        message_type VARCHAR(10) NOT NULL,
        network VARCHAR(20) NOT NULL,
        status VARCHAR(20) NOT NULL,
        request_reference VARCHAR(20) NOT NULL,
        transaction_reference VARCHAR(20) NOT NULL,
        value_date DATE NOT NULL,
        currency VARCHAR(3) NOT NULL,
        amount DECIMAL(15,2) NOT NULL,
        sender_institution_id VARCHAR(20) NOT NULL,
        receiver_institution_id VARCHAR(20) NOT NULL,
        validation_result JSON,
        validation_errors JSON,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        created_by VARCHAR(50)
    )
    ''';

    -- Create message_definition_mappings table
    EXECUTE '''
    CREATE TABLE IF NOT EXISTS message_definition_mappings (
        id BIGSERIAL PRIMARY KEY,
        message_type VARCHAR(10) NOT NULL,
        network VARCHAR(20) NOT NULL,
        version INT NOT NULL,
        field_mappings JSON NOT NULL,
        validation_rules JSON NOT NULL,
        is_active BOOLEAN NOT NULL
    )
    ''';

    -- Create indexes
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_institutions_id ON institutions(institution_id)'
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_messages_id ON messages(message_id)'
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_institution_id)'
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_messages_receiver_id ON messages(receiver_institution_id)'
    EXECUTE 'CREATE INDEX IF NOT EXISTS idx_message_definition_mappings_type_network ON message_definition_mappings(message_type, network)'
END;
$$;