-- Backfill BITs (reward_bits) for data created before the BIT layer existed.
-- Run after the backend has started once so Hibernate has created reward_bits and reward_transactions.bit_id.
BEGIN;

-- 1. ENROLL BIT per member, dated at enrollment, attributed to the program's HOST sponsor.
INSERT INTO reward_bits (tenant_id, program_id, bit_reference, bit_type, bit_category, member_id, bit_sponsor_id, billing_sponsor_id,
    channel, status, gross_amount, discount_amount, net_amount, currency, redemption_points_delta, recognition_points_delta,
    description, interaction_at, created_at)
SELECT m.tenant_id, p.id, 'ENROLL-' || m.external_user_id, 'ENROLL', 'ENROLLMENT', m.id, s.id, s.id,
    'BACKFILL', 'COMPLETED', 0, 0, 0, p.currency, 0, 0, 'Member enrolled (backfilled)', m.created_at, NOW()
FROM reward_members m
LEFT JOIN LATERAL (
    SELECT id, currency FROM reward_programs WHERE tenant_id = m.tenant_id ORDER BY created_at DESC LIMIT 1
) p ON TRUE
LEFT JOIN LATERAL (
    SELECT id FROM reward_sponsors WHERE tenant_id = m.tenant_id AND program_id = p.id ORDER BY (sponsor_type = 'HOST') DESC, name LIMIT 1
) s ON TRUE
ON CONFLICT (tenant_id, bit_reference, bit_type) DO NOTHING;

-- 2. One BIT per existing ledger transaction (expiry is a system ledger event, not a member interaction).
CREATE TEMP TABLE bit_backfill ON COMMIT DROP AS
SELECT t.id AS txn_id,
       t.tenant_id,
       COALESCE(t.reference_id, 'TXN-' || t.id) AS bit_reference,
       CASE t.transaction_type
           WHEN 'EARN' THEN CASE
               WHEN UPPER(t.event_type) IN ('PURCHASE','DINING','ONLINE_ORDER','HOTEL_STAY','FLIGHT_SEGMENT','CAR_RENTAL',
                                            'APP_LOGIN','SURVEY','REFERRAL','SOCIAL_SHARE','EVENT','PROMO_OFFERS') THEN UPPER(t.event_type)
               WHEN t.amount > 0 THEN 'PURCHASE' ELSE 'EVENT' END
           WHEN 'REDEEM' THEN 'REDEMPTION'
           WHEN 'REWARD_CLAIM' THEN 'REWARD_CLAIM'
           WHEN 'PRIVILEGE_CLAIM' THEN 'PRIVILEGE_CLAIM'
           WHEN 'DEAL_DISCOUNT' THEN 'DEAL'
           WHEN 'REVERSAL' THEN 'REVERSAL'
           WHEN 'EXPIRE' THEN 'EXPIRATION'
           WHEN 'ADJUSTMENT_CREDIT' THEN 'CS_ADJUSTMENT'
           WHEN 'ADJUSTMENT_DEBIT' THEN 'CS_ADJUSTMENT'
       END AS bit_type,
    CASE WHEN t.transaction_type IN ('REDEEM','REWARD_CLAIM','ADJUSTMENT_DEBIT','REVERSAL','EXPIRE') THEN -t.points ELSE t.points END AS redemption_delta,
       CASE WHEN t.transaction_type = 'REVERSAL' THEN -t.recognition_points ELSE t.recognition_points END AS recognition_delta
FROM reward_transactions t
WHERE t.bit_id IS NULL
    AND t.transaction_type IN ('EARN','REDEEM','REWARD_CLAIM','PRIVILEGE_CLAIM','DEAL_DISCOUNT','REVERSAL','EXPIRE','ADJUSTMENT_CREDIT','ADJUSTMENT_DEBIT');

INSERT INTO reward_bits (tenant_id, program_id, bit_reference, bit_type, bit_category, member_id, bit_sponsor_id, billing_sponsor_id,
    location_id, branch_id, channel, status, gross_amount, discount_amount, net_amount, currency, redemption_points_delta,
    recognition_points_delta, applied_policy_id, description, interaction_at, created_at)
SELECT t.tenant_id, t.program_id, b.bit_reference, b.bit_type,
       CASE
           WHEN b.bit_type IN ('PURCHASE','DINING','ONLINE_ORDER','HOTEL_STAY','FLIGHT_SEGMENT','CAR_RENTAL') THEN 'ACCRUAL'
           WHEN b.bit_type IN ('REDEMPTION','REWARD_CLAIM') THEN 'REDEMPTION'
           WHEN b.bit_type = 'PRIVILEGE_CLAIM' THEN 'PRIVILEGE'
           WHEN b.bit_type = 'DEAL' THEN 'DEAL'
           WHEN b.bit_type = 'REVERSAL' THEN 'CANCELLATION'
           WHEN b.bit_type = 'EXPIRATION' THEN 'EXPIRATION'
           WHEN b.bit_type = 'CS_ADJUSTMENT' THEN 'SERVICE'
           WHEN b.bit_type = 'ENROLL' THEN 'ENROLLMENT'
           WHEN b.bit_type = 'PROFILE_UPDATE' THEN 'PROFILE_UPDATE'
           WHEN b.bit_type = 'TIER_CHANGE' THEN 'TIER_CHANGE'
           ELSE 'ENGAGEMENT'
       END,
       t.member_id, t.sponsor_id, t.sponsor_id, t.location_id, t.branch_id, LEFT(t.channel, 30), 'COMPLETED',
       t.amount, COALESCE(t.discount_amount, 0), GREATEST(t.amount - COALESCE(t.discount_amount, 0), 0), p.currency,
       b.redemption_delta, b.recognition_delta, t.policy_id,
       'Backfilled from transaction #' || t.id, t.created_at, NOW()
FROM bit_backfill b
JOIN reward_transactions t ON t.id = b.txn_id
LEFT JOIN reward_programs p ON p.id = t.program_id
ON CONFLICT (tenant_id, bit_reference, bit_type) DO NOTHING;

UPDATE reward_transactions t
SET bit_id = bits.id
FROM bit_backfill b
JOIN reward_bits bits ON bits.tenant_id = b.tenant_id AND bits.bit_reference = b.bit_reference AND bits.bit_type = b.bit_type
WHERE t.id = b.txn_id AND t.bit_id IS NULL;

-- 3. Link reversal BITs to the BIT they reverse and flag the original.
UPDATE reward_bits rb
SET original_bit_id = orig.bit_id
FROM reward_transactions rev
JOIN reward_transactions orig ON orig.id = rev.original_transaction_id
WHERE rev.bit_id = rb.id AND rev.transaction_type = 'REVERSAL' AND rb.original_bit_id IS NULL AND orig.bit_id IS NOT NULL;

UPDATE reward_bits ob
SET status = CASE WHEN rev.points >= orig.points THEN 'REVERSED' ELSE 'PARTIALLY_REVERSED' END
FROM reward_transactions rev
JOIN reward_transactions orig ON orig.id = rev.original_transaction_id
WHERE ob.id = orig.bit_id AND rev.transaction_type = 'REVERSAL' AND ob.status = 'COMPLETED';

COMMIT;
