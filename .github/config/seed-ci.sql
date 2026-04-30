-- =============================================================================
-- CI Integration-Test Seed Data
-- =============================================================================
-- This file runs AFTER Liquibase migrations have applied the full schema.
-- It inserts the minimum set of synthetic reference objects that integration
-- tests need in order to exercise each user-role path.
--
-- HOW TO EXTEND
--   Add new INSERT blocks in the relevant section below.  Each section is
--   self-contained and idempotent:  re-running this script against a database
--   that already contains these rows is safe (nothing will be duplicated).
--
-- SYNTHETIC DATA ONLY
--   Do not add real email addresses, names, tokens, or credentials.
--   All example.com addresses are RFC-5737 reserved and will never resolve.
-- =============================================================================


-- ===========================================================================
-- 1. USERS
--    One representative user per application role.
--    Add more rows here when you need additional actors in your tests.
-- ===========================================================================

INSERT INTO users (email, display_name, create_date, email_preference)
VALUES
  -- System-level roles
  ('ci-admin@example.com',            'CI Admin',            NOW(), false),
  ('ci-signing-official@example.com', 'CI Signing Official', NOW(), false),
  ('ci-it-director@example.com',      'CI IT Director',      NOW(), false),
  ('ci-data-submitter@example.com',   'CI Data Submitter',   NOW(), false),
  ('ci-researcher@example.com',       'CI Researcher',       NOW(), false),

  -- DAC-scoped roles (chair/member assignment happens in section 5)
  ('ci-chair@example.com',            'CI DAC Chair',        NOW(), false),
  ('ci-member@example.com',           'CI DAC Member',       NOW(), false)
ON CONFLICT (email) DO NOTHING;


-- ===========================================================================
-- 2. INSTITUTIONS
--    A single test institution linked to the CI admin as creator.
--    Add more rows here when your tests require multiple institutions.
-- ===========================================================================

INSERT INTO institution (institution_name, it_director_name, it_director_email, create_user, create_date)
SELECT
  'CI Test Institution',
  'CI IT Director',
  'ci-it-director@example.com',
  u.user_id,
  NOW()
FROM users u
WHERE u.email = 'ci-admin@example.com'
ON CONFLICT (institution_name) DO NOTHING;

-- Link the researcher and signing official to the test institution so that
-- library-card and signing-official workflows have valid FK references.
UPDATE users
SET institution_id = (
  SELECT institution_id FROM institution
  WHERE institution_name = 'CI Test Institution'
)
WHERE email IN (
  'ci-researcher@example.com',
  'ci-signing-official@example.com'
)
  AND institution_id IS NULL;


-- ===========================================================================
-- 3. USER ROLES  (non-DAC)
--    Associates each user with their primary application-level role.
--    Use separate INSERT blocks for additional role assignments.
-- ===========================================================================

INSERT INTO user_role (role_id, user_id)
SELECT r.role_id, u.user_id
FROM roles r
JOIN users u ON TRUE
WHERE (r.name = 'Admin'          AND u.email = 'ci-admin@example.com')
   OR (r.name = 'SigningOfficial' AND u.email = 'ci-signing-official@example.com')
   OR (r.name = 'ITDirector'      AND u.email = 'ci-it-director@example.com')
   OR (r.name = 'DataSubmitter'   AND u.email = 'ci-data-submitter@example.com')
   OR (r.name = 'Researcher'      AND u.email = 'ci-researcher@example.com')
  -- skip rows that already exist
  AND NOT EXISTS (
    SELECT 1 FROM user_role ur2
    WHERE ur2.user_id = u.user_id AND ur2.role_id = r.role_id AND ur2.dac_id IS NULL
  );


-- ===========================================================================
-- 4. DAC
--    A single test DAC.  Repeated runs are safe: the INSERT is skipped when
--    a DAC with the same name already exists.
--    An audit row (action = CREATE) is written atomically alongside the DAC.
-- ===========================================================================

DO $$
DECLARE
  v_admin_id  bigint;
  v_dac_id    bigint;
BEGIN
  SELECT user_id INTO v_admin_id FROM users WHERE email = 'ci-admin@example.com';

  -- Insert the DAC only if it does not already exist.
  SELECT dac_id INTO v_dac_id FROM dac WHERE name = 'CI Test DAC';

  IF v_dac_id IS NULL THEN
    INSERT INTO dac (name, description, create_date, deleted)
    VALUES ('CI Test DAC', 'Test DAC for CI integration tests', NOW(), false)
    RETURNING dac_id INTO v_dac_id;

    INSERT INTO dac_audit (dac_id, user_id, action, action_date)
    VALUES (v_dac_id, v_admin_id, 'CREATE', NOW());
  END IF;
END $$;


-- ===========================================================================
-- 5. DAC MEMBER ASSIGNMENTS
--    Assigns the CI chair and CI member to the test DAC.
--    Extend this section to add more DAC-scoped role assignments.
-- ===========================================================================

INSERT INTO user_role (role_id, user_id, dac_id)
SELECT r.role_id, u.user_id, d.dac_id
FROM roles r
JOIN users u ON TRUE
JOIN dac   d ON d.name = 'CI Test DAC'
WHERE (r.name = 'Chairperson' AND u.email = 'ci-chair@example.com')
   OR (r.name = 'Member'      AND u.email = 'ci-member@example.com')
  AND NOT EXISTS (
    SELECT 1 FROM user_role ur2
    WHERE ur2.user_id = u.user_id AND ur2.role_id = r.role_id AND ur2.dac_id = d.dac_id
  );

-- Write DAC audit entries for the member additions.
INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
SELECT
  d.dac_id,
  admin_u.user_id,   -- actor: CI admin
  u.user_id,         -- subject: user being added
  r.role_id,
  'ADD',
  NOW()
FROM roles r
JOIN users        u      ON TRUE
JOIN dac          d      ON d.name = 'CI Test DAC'
JOIN users        admin_u ON admin_u.email = 'ci-admin@example.com'
WHERE (r.name = 'Chairperson' AND u.email = 'ci-chair@example.com')
   OR (r.name = 'Member'      AND u.email = 'ci-member@example.com')
  AND NOT EXISTS (
    SELECT 1 FROM dac_audit da
    WHERE da.dac_id = d.dac_id
      AND da.affected_user_id = u.user_id
      AND da.action = 'ADD'
  );


-- ===========================================================================
-- ADD CUSTOM APPLICATION DATA BELOW
-- ===========================================================================
-- Examples of what you might add:
--
--   * Datasets and dataset properties
--   * Data Access Requests (DAR collections)
--   * Library Cards linked to ci-researcher@example.com
--   * Data Access Agreements (DAA) linked to the CI Test DAC
--   * Feature flags
--
-- Follow the same idempotency pattern: use ON CONFLICT DO NOTHING or
-- WHERE NOT EXISTS so that repeated runs against the same database are safe.
-- ===========================================================================

