-- Add diagnostics and source metadata required by the program-wide BIT activity feed.
-- Run after reward_bits has been created by the application.
BEGIN;

ALTER TABLE reward_bits
    ALTER COLUMN status TYPE varchar(30),
    ADD COLUMN IF NOT EXISTS error_code varchar(50),
    ADD COLUMN IF NOT EXISTS error_message varchar(2000),
    ADD COLUMN IF NOT EXISTS bit_source varchar(30);

UPDATE reward_bits
SET bit_source = channel
WHERE bit_source IS NULL;

CREATE INDEX IF NOT EXISTS idx_reward_bits_tenant_interaction_time
    ON reward_bits (tenant_id, interaction_at DESC);

CREATE INDEX IF NOT EXISTS idx_reward_bits_tenant_member_interaction_time
    ON reward_bits (tenant_id, member_id, interaction_at DESC);

COMMIT;