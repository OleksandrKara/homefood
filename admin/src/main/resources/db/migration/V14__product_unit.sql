-- Countable unit noun per product (банка, шт, кг...), replacing the word hardcoded everywhere in
-- templates. Existing products are all jarred goods, so backfill 'банка' for them; the app-level
-- default for brand-new products is 'шт' (see Product.unit).
ALTER TABLE products ADD COLUMN unit VARCHAR(50) NOT NULL DEFAULT 'банка';
