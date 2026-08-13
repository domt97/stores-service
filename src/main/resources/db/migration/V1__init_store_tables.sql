CREATE TABLE IF NOT EXISTS stores (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    owner_id    UUID         NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(50),
    status      VARCHAR(30)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_stores_code UNIQUE(code)
);

CREATE SEQUENCE IF NOT EXISTS store_address_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

CREATE TABLE IF NOT EXISTS store_addresses (
    id             BIGINT PRIMARY KEY DEFAULT nextval('store_address_seq'),
    store_id       UUID NOT NULL,
    phone          VARCHAR(50),
    address_line1  VARCHAR(255),
    address_line2  VARCHAR(255),
    ward           VARCHAR(100),
    district       VARCHAR(100),
    province       VARCHAR(100),
    city           VARCHAR(100),
    country        VARCHAR(100),
    postal_code    VARCHAR(20),

    CONSTRAINT fk_store_addresses_store
    FOREIGN KEY(store_id) REFERENCES stores(id),

    CONSTRAINT uk_store_addresses_store
    UNIQUE(store_id)
);

CREATE SEQUENCE IF NOT EXISTS store_config_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

CREATE TABLE IF NOT EXISTS store_configs (
    id                          BIGINT PRIMARY KEY DEFAULT nextval('store_config_seq'),
    store_id UUID               NOT NULL,
    auto_accept_order           BOOLEAN NOT NULL DEFAULT TRUE,
    allow_preorder              BOOLEAN NOT NULL DEFAULT FALSE,
    opening_time                TIME,
    closing_time                TIME,
    timezone                    VARCHAR(50) NOT NULL,
    currency                    VARCHAR(3) NOT NULL,
    max_orders_per_day          INT,
    preparation_time_minutes    INT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_configs_store
       FOREIGN KEY(store_id)
           REFERENCES stores(id),
    CONSTRAINT uk_store_configs_store
       UNIQUE(store_id)
);

CREATE SEQUENCE IF NOT EXISTS store_business_hour_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 20;

CREATE TABLE store_business_hours (
    id              BIGINT PRIMARY KEY DEFAULT nextval('store_business_hour_seq'),
    store_id        UUID NOT NULL,
    day_of_week     VARCHAR(20) NOT NULL,
    opening_time    TIME,
    closing_time    TIME,
    is_closed       BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_business_hours_store
      FOREIGN KEY(store_id)
          REFERENCES stores(id)
);