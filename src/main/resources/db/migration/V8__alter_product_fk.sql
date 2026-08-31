ALTER TABLE product_skus DROP CONSTRAINT fk_product_skus_product;
ALTER TABLE product_skus
    ADD CONSTRAINT fk_product_skus_product
        FOREIGN KEY (product_id)
            REFERENCES store_products (id)
            ON DELETE CASCADE;

ALTER TABLE product_images DROP CONSTRAINT fk_product_images_product;
ALTER TABLE product_images
    ADD CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id)
            REFERENCES store_products (id)
            ON DELETE CASCADE;