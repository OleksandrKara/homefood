-- Tracks whose money paid for an expense (investor vs. working capital), separate from the
-- existing category (what the expense was for). All existing expenses are backfilled as
-- investor-funded per business owner - to be corrected by hand for the one that was actually
-- paid from working capital.
ALTER TABLE expenses ADD COLUMN funding_source VARCHAR(20) NOT NULL DEFAULT 'INVESTOR';
