ALTER TABLE orders ADD COLUMN notes TEXT;

CREATE TABLE districts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO districts (name)
SELECT DISTINCT district FROM orders WHERE district IS NOT NULL AND district <> '';
