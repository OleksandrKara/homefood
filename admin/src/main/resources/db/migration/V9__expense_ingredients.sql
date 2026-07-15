CREATE TABLE expense_ingredients (
    id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expenses(id),
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id),
    quantity NUMERIC(10,2) NOT NULL,
    UNIQUE (expense_id, ingredient_id)
);

INSERT INTO expense_ingredients (expense_id, ingredient_id, quantity)
SELECT id, ingredient_id, quantity FROM expenses WHERE ingredient_id IS NOT NULL AND quantity IS NOT NULL;

ALTER TABLE expenses DROP COLUMN ingredient_id;
ALTER TABLE expenses DROP COLUMN quantity;
