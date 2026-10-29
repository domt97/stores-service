CREATE TABLE IF NOT EXISTS store_products
(
    id          UUID PRIMARY KEY,
    store_id    UUID         NOT NULL,

    name        VARCHAR(255) NOT NULL,
    description TEXT,

    category_id UUID,

    status      VARCHAR(30)  NOT NULL,

    -- Optimistic version for update-product; written only by the guarded bump (StoreProductPersistenceAdapter).
    version     BIGINT       NOT NULL DEFAULT 0,

    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT fk_store_products_store
        FOREIGN KEY (store_id)
            REFERENCES stores (id),

    -- Target of fk_product_skus_product_store: a SKU's store is always its product's store.
    CONSTRAINT uk_store_products_id_store
        UNIQUE (id, store_id)
);

CREATE INDEX idx_store_products_store
    ON store_products (store_id);

CREATE INDEX idx_store_products_store_status
    ON store_products (store_id, status);


CREATE TABLE IF NOT EXISTS product_skus
(
    id          UUID PRIMARY KEY,
    product_id  UUID         NOT NULL,
    store_id    UUID         NOT NULL,

    sku         VARCHAR(100) NOT NULL,
    name        VARCHAR(255),

    price       NUMERIC(18,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL,

    weight      NUMERIC(10,2),
    length      NUMERIC(10,2),
    width       NUMERIC(10,2),
    height      NUMERIC(10,2),

    status      VARCHAR(30) NOT NULL,

    -- Soft remove (BR-CAT-09): the row and its code stay reserved in the store.
    removed_at  TIMESTAMP,

    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT fk_product_skus_product
        FOREIGN KEY (product_id)
            REFERENCES store_products (id),

    CONSTRAINT fk_product_skus_product_store
        FOREIGN KEY (product_id, store_id)
            REFERENCES store_products (id, store_id)
            ON DELETE CASCADE,

    -- BR-CAT-02: a SKU code is unique within a store, removed codes included (BR-CAT-09).
    CONSTRAINT uk_product_skus_store_sku
        UNIQUE (store_id, sku)
);

CREATE INDEX idx_product_skus_product
    ON product_skus (product_id);


CREATE SEQUENCE IF NOT EXISTS product_image_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

CREATE TABLE IF NOT EXISTS product_images
(
    id            BIGINT PRIMARY KEY DEFAULT nextval('product_image_seq'),
    product_id    UUID NOT NULL,

    image_url     TEXT NOT NULL,
    display_order INT,

    created_at    TIMESTAMP NOT NULL,

    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id)
            REFERENCES store_products (id)
);

CREATE INDEX idx_product_images_product
    ON product_images (product_id);