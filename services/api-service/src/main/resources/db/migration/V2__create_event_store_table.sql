-- Create event store table for event sourcing
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE event_store (
    event_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    aggregate_id VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    sequence_number BIGINT,
    correlation_id VARCHAR(100),
    initiated_by VARCHAR(100),
    occurred_at TIMESTAMP NOT NULL,
    stored_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_data JSONB NOT NULL,
    metadata JSONB
);

-- Create indexes for efficient querying
CREATE INDEX idx_event_store_aggregate_id ON event_store(aggregate_id);
CREATE INDEX idx_event_store_aggregate_type ON event_store(aggregate_type);
CREATE INDEX idx_event_store_event_type ON event_store(event_type);
CREATE INDEX idx_event_store_correlation_id ON event_store(correlation_id);
CREATE INDEX idx_event_store_occurred_at ON event_store(occurred_at);
CREATE INDEX idx_event_store_stored_at ON event_store(stored_at);
CREATE UNIQUE INDEX idx_event_store_sequence_number ON event_store(aggregate_id, sequence_number);

-- Create partial index for recent events (commonly queried)
CREATE INDEX idx_event_store_recent ON event_store(occurred_at DESC) WHERE occurred_at > NOW() - INTERVAL '7 days';

-- Create GIN index on JSONB columns for efficient JSON queries
CREATE INDEX idx_event_store_event_data_gin ON event_store USING GIN (event_data);
CREATE INDEX idx_event_store_metadata_gin ON event_store USING GIN (metadata);

-- Add constraints
ALTER TABLE event_store ADD CONSTRAINT chk_event_version_positive CHECK (event_version > 0);
ALTER TABLE event_store ADD CONSTRAINT chk_sequence_number_positive CHECK (sequence_number IS NULL OR sequence_number > 0);

-- Create a function to automatically update stored_at timestamp
CREATE OR REPLACE FUNCTION update_stored_at_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.stored_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update stored_at
CREATE TRIGGER trigger_event_store_update_stored_at
    BEFORE UPDATE ON event_store
    FOR EACH ROW
    EXECUTE PROCEDURE update_stored_at_timestamp();

-- Grant permissions (adjust as needed)
-- GRANT SELECT, INSERT ON event_store TO speechtotext_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO speechtotext_app;
