ALTER TABLE idempotency
    ADD COLUMN IF NOT EXISTS owner_id VARCHAR(128);

ALTER TABLE idempotency
    ADD COLUMN IF NOT EXISTS lease_until TIMESTAMPTZ;

ALTER TABLE idempotency
    ADD COLUMN IF NOT EXISTS last_claim_date TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_idempotency_status_claim_window
    ON idempotency (status, next_attempt_date, lease_until, create_date);

CREATE INDEX IF NOT EXISTS idx_idempotency_expired_leases
    ON idempotency (lease_until)
    WHERE owner_id IS NOT NULL;

ALTER TABLE kafka_event_outbox
    ADD COLUMN IF NOT EXISTS owner_id VARCHAR(128);

ALTER TABLE kafka_event_outbox
    ADD COLUMN IF NOT EXISTS lease_until TIMESTAMPTZ;

ALTER TABLE kafka_event_outbox
    ADD COLUMN IF NOT EXISTS last_claim_date TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_kafka_event_outbox_claim_window
    ON kafka_event_outbox (status, next_attempt_date, lease_until, create_date);

CREATE INDEX IF NOT EXISTS idx_kafka_event_outbox_expired_leases
    ON kafka_event_outbox (lease_until)
    WHERE owner_id IS NOT NULL;
