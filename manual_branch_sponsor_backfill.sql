-- Link outlet branches to CHILD sponsors and fix historic BIT rows (run after deploying branch.sponsorId).
BEGIN;

INSERT INTO reward_sponsors (tenant_id, program_id, parent_sponsor_id, name, sponsor_code, sponsor_type, status, created_at)
SELECT b.tenant_id,
       p.id,
       host.id,
       TRIM(host.name || ' ' || INITCAP(b.city)),
       UPPER(REGEXP_REPLACE(b.code, '[^A-Za-z0-9]+', '_', 'g')),
       'CHILD',
       'ACTIVE',
       NOW()
FROM reward_branches b
JOIN reward_programs p ON p.tenant_id = b.tenant_id
JOIN reward_sponsors host ON host.tenant_id = b.tenant_id AND host.program_id = p.id AND host.sponsor_type = 'HOST'
WHERE b.code <> 'DEFAULT_MAIN'
  AND b.sponsor_id IS NULL
  AND b.city IS NOT NULL
ON CONFLICT DO NOTHING;

UPDATE reward_branches b
SET sponsor_id = s.id
FROM reward_sponsors s
WHERE b.sponsor_id IS NULL
  AND s.tenant_id = b.tenant_id
  AND s.sponsor_code = UPPER(REGEXP_REPLACE(b.code, '[^A-Za-z0-9]+', '_', 'g'))
  AND s.sponsor_type = 'CHILD';

UPDATE reward_bits bit
SET bit_sponsor_id = b.sponsor_id
FROM reward_branches b
WHERE bit.branch_id = b.id
  AND b.sponsor_id IS NOT NULL
  AND bit.bit_sponsor_id IS DISTINCT FROM b.sponsor_id;

UPDATE reward_transactions t
SET sponsor_id = b.sponsor_id
FROM reward_branches b
WHERE t.branch_id = b.id
  AND b.sponsor_id IS NOT NULL
  AND t.sponsor_id IS DISTINCT FROM b.sponsor_id;

COMMIT;
