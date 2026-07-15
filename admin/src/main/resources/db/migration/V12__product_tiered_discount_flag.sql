ALTER TABLE products ADD COLUMN tiered_discount_enabled BOOLEAN NOT NULL DEFAULT false;

-- The original "jar 2-3 discounted $1" pricing was designed for Квашеная капуста specifically -
-- keep it there exactly as before; every other (new) product uses flat price x quantity.
UPDATE products SET tiered_discount_enabled = true WHERE name = 'Квашеная капуста';
