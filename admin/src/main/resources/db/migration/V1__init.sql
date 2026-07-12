CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    size_label VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    delivery_type VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(500),
    district VARCHAR(255),
    delivery_details TEXT,
    delivery_date DATE,
    delivery_time TIME,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_client_id ON orders(client_id);
CREATE INDEX idx_orders_delivery_date ON orders(delivery_date);

INSERT INTO products (name, size_label) VALUES ('Капуста', '1lb');
