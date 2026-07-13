ALTER TABLE orders ADD COLUMN archived BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_orders_archived ON orders(archived);
