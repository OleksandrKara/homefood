ALTER TABLE clients ADD COLUMN archived BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_clients_archived ON clients(archived);
