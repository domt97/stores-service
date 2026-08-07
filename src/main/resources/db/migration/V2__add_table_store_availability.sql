CREATE TABLE IF NOT EXISTS store_availability
(
    id UUID          PRIMARY KEY,
    store_id         UUID NOT NULL,
    type             VARCHAR(30) NOT NULL,
    cancelled        BOOLEAN NOT NULL DEFAULT FALSE,
    start_time       TIMESTAMP NOT NULL,
    end_time         TIMESTAMP,
    reason           VARCHAR(255),
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL,

    CONSTRAINT fk_store_availability_store
        FOREIGN KEY (store_id)
            REFERENCES stores(id)
);

CREATE INDEX idx_store_availability_store
    ON store_availability(store_id);

CREATE INDEX idx_store_availability_lookup
    ON store_availability(store_id, cancelled, start_time, end_time);