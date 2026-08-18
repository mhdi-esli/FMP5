-- Enrich message_definition_mappings with field mappings and validation rules
-- as structured JSONB data.

-- Update MT200/SWIFT definition v1 with field mappings
UPDATE message_definition_mappings
SET field_mappings = '[
    {"fieldName": "amount", "swiftTag": ":32A:", "sepaField": "Amount", "required": true, "maxLength": 20, "dataType": "decimal", "validationPattern": "^[0-9]+\\\\.[0-9]{2}$"},
    {"fieldName": "currency", "swiftTag": ":32A:", "sepaField": "Currency", "required": true, "maxLength": 3, "dataType": "string", "validationPattern": "^[A-Z]{3}$"},
    {"fieldName": "valueDate", "swiftTag": ":32A:", "sepaField": "ValueDate", "required": true, "maxLength": 8, "dataType": "date", "validationPattern": "^[0-9]{4}-[0-9]{2}-[0-9]{2}$"},
    {"fieldName": "senderBIC", "swiftTag": ":52A:", "sepaField": "SenderBIC", "required": true, "maxLength": 11, "dataType": "string"},
    {"fieldName": "receiverBIC", "swiftTag": ":57A:", "sepaField": "ReceiverBIC", "required": true, "maxLength": 11, "dataType": "string"},
    {"fieldName": "reference", "swiftTag": ":20:", "sepaField": "Reference", "required": true, "maxLength": 35, "dataType": "string"},
    {"fieldName": "narrative", "swiftTag": ":70:", "sepaField": "Narrative", "required": false, "maxLength": 140, "dataType": "string"}
]'::jsonb,
    validation_rules = '[
        {"ruleName": "amount_positive", "enabled": true, "params": {"min": "0.01"}},
        {"ruleName": "currency_iso", "enabled": true, "params": {}},
        {"ruleName": "value_date_future", "enabled": false, "params": {"maxDays": "90"}}
    ]'::jsonb,
    updated_at = CURRENT_TIMESTAMP
WHERE message_type = '200' AND network = 'SWIFT' AND version = 1;

-- Update MT200/SEPA definition v1 with field mappings
UPDATE message_definition_mappings
SET field_mappings = '[
    {"fieldName": "amount", "swiftTag": ":32A:", "sepaField": "Amount", "required": true, "maxLength": 20, "dataType": "decimal", "validationPattern": "^[0-9]+\\\\.[0-9]{2}$"},
    {"fieldName": "currency", "swiftTag": ":32A:", "sepaField": "Currency", "required": true, "maxLength": 3, "dataType": "string", "validationPattern": "^[A-Z]{3}$"},
    {"fieldName": "valueDate", "swiftTag": ":32A:", "sepaField": "ValueDate", "required": true, "maxLength": 8, "dataType": "date", "validationPattern": "^[0-9]{4}-[0-9]{2}-[0-9]{2}$"},
    {"fieldName": "reference", "swiftTag": ":20:", "sepaField": "Reference", "required": true, "maxLength": 35, "dataType": "string"}
]'::jsonb,
    validation_rules = '[
        {"ruleName": "amount_positive", "enabled": true, "params": {"min": "0.01"}},
        {"ruleName": "currency_iso", "enabled": true, "params": {}},
        {"ruleName": "sepa_compliance", "enabled": false, "params": {"scheme": "SEPA_CREDIT_TRANSFER"}}
    ]'::jsonb,
    updated_at = CURRENT_TIMESTAMP
WHERE message_type = '200' AND network = 'SEPA' AND version = 1;
