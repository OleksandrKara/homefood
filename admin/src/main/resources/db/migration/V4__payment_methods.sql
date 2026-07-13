ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50);

CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO payment_methods (name) VALUES ('Cash'), ('Zelle'), ('Venmo');
