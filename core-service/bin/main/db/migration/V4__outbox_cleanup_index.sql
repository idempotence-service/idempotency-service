CREATE INDEX IF NOT EXISTS idx_kafka_event_outbox_status_update_date
    ON kafka_event_outbox (status, update_date);
