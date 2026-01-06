-- Phase 4: admin + integrations + holiday deprecation metadata
ALTER TABLE holidays
    ADD COLUMN IF NOT EXISTS deprecation_reason TEXT;

-- Ensure only one active config per integration type to simplify fail-fast lookups
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'uq_integration_active_type'
    ) THEN
        EXECUTE 'CREATE UNIQUE INDEX uq_integration_active_type
                 ON integration_configs(type)
                 WHERE state = ''CONFIGURED''';
    END IF;
END$$;
