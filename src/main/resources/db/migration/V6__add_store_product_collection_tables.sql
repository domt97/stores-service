CREATE TABLE IF NOT EXISTS store_collections
(
    id          UUID PRIMARY KEY,
    store_id    UUID         NOT NULL,

    name        VARCHAR(255) NOT NULL,
    description TEXT,

    status      VARCHAR(30)  NOT NULL,

    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT fk_store_collections_store
        FOREIGN KEY (store_id)
            REFERENCES stores (id)
);

CREATE INDEX idx_store_collections_store
    ON store_collections (store_id);

CREATE INDEX idx_store_collections_store_status
    ON store_collections (store_id, status);



CREATE SEQUENCE IF NOT EXISTS product_collection_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;


CREATE TABLE IF NOT EXISTS product_collections
(
    id            BIGINT PRIMARY KEY DEFAULT nextval('product_collection_seq'),
    collection_id UUID NOT NULL,
    product_id    UUID NOT NULL,

    created_at    TIMESTAMP NOT NULL,

    CONSTRAINT fk_product_collections_collection
        FOREIGN KEY (collection_id)
            REFERENCES store_collections (id),

    CONSTRAINT fk_product_collections_product
        FOREIGN KEY (product_id)
            REFERENCES store_products (id),

    CONSTRAINT uk_product_collections_collection_product
        UNIQUE (collection_id, product_id)
);

CREATE INDEX idx_product_collections_collection
    ON product_collections (collection_id);