-- An order can now be paid via several methods at once (e.g. partially cash, partially Venmo) -
-- move payment_method off orders into a line-items-style table. Every existing order with a
-- recorded payment method becomes a single payment row for its full total_price.
CREATE TABLE order_payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_method VARCHAR(50) NOT NULL,
    amount NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_order_payments_order_id ON order_payments(order_id);

INSERT INTO order_payments (order_id, payment_method, amount)
SELECT id, payment_method, COALESCE(total_price, 0)
FROM orders
WHERE payment_method IS NOT NULL AND payment_method <> '';

ALTER TABLE orders DROP COLUMN payment_method;
