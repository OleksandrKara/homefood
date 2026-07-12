ALTER TABLE products ADD COLUMN base_price NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN total_price NUMERIC(10,2);

UPDATE products SET base_price = 5.00 WHERE name = 'Капуста';
