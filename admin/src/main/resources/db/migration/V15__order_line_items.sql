-- An order can now hold several products instead of exactly one - move product/quantity off
-- orders into a line-items table. Every existing order becomes a single-line-item order,
-- preserving its current product/quantity/price exactly.
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    line_total NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);

INSERT INTO order_items (order_id, product_id, quantity, line_total)
SELECT id, product_id, quantity, COALESCE(total_price, 0)
FROM orders;

ALTER TABLE orders DROP COLUMN product_id;
ALTER TABLE orders DROP COLUMN quantity;
