-- Hibernate cannot ADD these NOT NULL columns to a populated table, so add them with defaults.
ALTER TABLE reward_programs ADD COLUMN IF NOT EXISTS expiry_type varchar(255) NOT NULL DEFAULT 'ROLLING';
ALTER TABLE reward_programs ADD COLUMN IF NOT EXISTS expiry_months integer NOT NULL DEFAULT 12;
ALTER TABLE reward_programs ADD COLUMN IF NOT EXISTS expiry_warning_days integer NOT NULL DEFAULT 30;

-- Align reward_wallet_history.account_type with the entity's varchar(32) default 'REDEMPTION'
-- (Hibernate's generated ALTER for this column is invalid Postgres syntax).
UPDATE reward_wallet_history SET account_type = 'REDEMPTION' WHERE account_type IS NULL;
ALTER TABLE reward_wallet_history ALTER COLUMN account_type TYPE varchar(32);
ALTER TABLE reward_wallet_history ALTER COLUMN account_type SET DEFAULT 'REDEMPTION';
ALTER TABLE reward_wallet_history ALTER COLUMN account_type SET NOT NULL;
