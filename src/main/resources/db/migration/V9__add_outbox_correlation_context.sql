-- Correlation keys of the request that wrote the row (oms-core MdcPropagation, ADR 0004), e.g. {"requestId": "abc"}.
-- Debezium's EventRouter copies it to the Kafka header "correlationContext". NOT NULL with a constant default: no
-- table rewrite on Postgres 11+, existing rows read as '{}', and the header is never null.
ALTER TABLE outbox_events
    ADD COLUMN correlation_context JSONB NOT NULL DEFAULT '{}'::jsonb;
