ALTER TABLE production_batches ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DONE';
ALTER TABLE production_batches ADD COLUMN pickup_location VARCHAR(500);
ALTER TABLE production_batches ADD COLUMN pickup_window VARCHAR(255);
