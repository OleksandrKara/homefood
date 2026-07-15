CREATE TABLE shop_settings (
    id BIGINT PRIMARY KEY,
    pickup_address VARCHAR(500),
    CONSTRAINT shop_settings_singleton CHECK (id = 1)
);

INSERT INTO shop_settings (id, pickup_address) VALUES (1, NULL);

ALTER TABLE products ADD COLUMN image_url VARCHAR(500);
