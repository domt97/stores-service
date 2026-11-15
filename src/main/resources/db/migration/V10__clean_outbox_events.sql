-- From now on stores-service inserts each outbox row and deletes it in the same transaction (oms-workspace open
-- question 19): Debezium publishes the INSERT from the WAL, the table stays empty. This removes the rows written before.
-- Safe for the connector: every row's INSERT is already in the WAL, so it is published whether or not the row still
-- exists. Run the development repo's kafka-connect/sql/store-outbox-cdc.sql first on a stack with the connector
-- postgres-cdc: its insert-only publication does not send these DELETEs.
-- The columns status, published_at, retry_count, last_error and the index idx_outbox_events_status are kept for now
-- (dropped in a later contract step).
DELETE FROM outbox_events;
