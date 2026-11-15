-- BR-STO-05: auto-accept is off by default for a new store (open question 62). Hibernate inserts every column, so only
-- hand-written SQL used the old TRUE default; existing rows keep their value. Catalog-only change, no table rewrite.
ALTER TABLE store_configs ALTER COLUMN auto_accept_order SET DEFAULT FALSE;
