ALTER TABLE idempotency
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE idempotency
    ADD COLUMN IF NOT EXISTS next_attempt_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_idempotency_status_next_attempt_date
    ON idempotency (status, next_attempt_date, create_date);

ALTER TABLE kafka_event_outbox
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE kafka_event_outbox
    ADD COLUMN IF NOT EXISTS next_attempt_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_kafka_event_outbox_status_next_attempt_date
    ON kafka_event_outbox (status, next_attempt_date, create_date);
