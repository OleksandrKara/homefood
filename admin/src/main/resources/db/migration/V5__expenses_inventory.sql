CREATE TABLE ingredients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    stock_quantity NUMERIC(10,2) NOT NULL DEFAULT 0
);

CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(20) NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    expense_date DATE NOT NULL,
    ingredient_id BIGINT REFERENCES ingredients(id),
    quantity NUMERIC(10,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE recipes (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id),
    quantity_per_unit NUMERIC(10,2) NOT NULL,
    UNIQUE (product_id, ingredient_id)
);

CREATE TABLE production_batches (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity_produced INT NOT NULL,
    batch_date DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE products ADD COLUMN stock_quantity INT NOT NULL DEFAULT 0;

CREATE INDEX idx_expenses_date ON expenses(expense_date);
CREATE INDEX idx_production_batches_product ON production_batches(product_id);
