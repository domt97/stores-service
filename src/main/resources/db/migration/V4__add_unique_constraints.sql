ALTER TABLE stores DROP CONSTRAINT uk_stores_code ;
ALTER TABLE stores ADD CONSTRAINT uk_merchant_store_code UNIQUE(merchant_id, code);