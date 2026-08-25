CREATE TABLE outbox_events
(
    id             UUID PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID NOT NULL,

    event_type     VARCHAR(100) NOT NULL,

    payload        JSONB NOT NULL,

    status         VARCHAR(30) NOT NULL,

    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMP,

    retry_count    INT NOT NULL DEFAULT 0,
    last_error     TEXT
);

CREATE INDEX idx_outbox_events_status
    ON outbox_events (status, created_at);