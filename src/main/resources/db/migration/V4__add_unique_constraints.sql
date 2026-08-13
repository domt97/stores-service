ALTER TABLE stores DROP CONSTRAINT uk_stores_code ;
ALTER TABLE stores ADD CONSTRAINT uk_tenant_store_code UNIQUE(tenant_id, code);