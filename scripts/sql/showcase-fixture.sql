-- Synthetic Data Showcase fixture for request-flow testing in a LOCAL database (DT-4358, S4.2).
--
-- Run it with scripts/seed-showcase-fixture.sh, which only targets the `localdb` container. Never
-- run it against a shared environment.
--
-- The fixture's own rows use IDs from 990001, far above the dev dump's sequences, so they do not
-- collide with dumped rows; once the dump's sequences reach that range, the seed stops. The duos-ui request-flow showcase names these aliases (DUOS-990001 ...).
-- As the app does when it registers a dataset, the fixture's datasets get asset catalog entries,
-- and the alias sequence moves past 990005, so the sequence never issues a fixture alias again: a
-- dataset registered locally afterwards gets 990006 or higher. Each insert is an upsert on its
-- primary key: a second run restores
-- the fixture's values, and keeps any data access request a tester has made against it. The
-- statements name no schema: the local `consent` user resolves to the `consent` schema ("$user").
--
-- | Alias       | Access     | Showcase lookup | Why                                      |
-- |-------------|------------|-----------------|------------------------------------------|
-- | DUOS-990001 | controlled | eligible        | DAC-approved, public study               |
-- | DUOS-990002 | open       | eligible        | open access, public study                |
-- | DUOS-990003 | external   | eligible        | external access, public study            |
-- | DUOS-990004 | controlled | not eligible    | no DAC approval                          |
-- | DUOS-990005 | controlled | not eligible    | DAC-approved, but the study is not public |
--
-- The eligibility rule is DatasetDAO.findShowcaseSummaries. Dictionary keys are looked up by name.

BEGIN;

-- Refuses to run unless the session names the local target. scripts/seed-showcase-fixture.sh sets
-- it (PGOPTIONS), and nothing else does, so pasting this file into a session on a shared or
-- production database aborts the transaction before any write.
--
-- It also refuses two databases it would seed wrongly:
-- - One migrated to the 2026-08-10 changeset but not the 2026-09-17 one. Its dataset_alias_allocate
--   trigger replaces every inserted alias, so the fixture would get aliases other than DUOS-990001
--   to DUOS-990005.
-- - One whose ID sequences have reached the fixture's IDs. The app may already have given a row of
--   its own one of them, and the upserts would overwrite it. The fixture sets its IDs by hand, so it
--   never moves these sequences itself, and edits the app makes to fixture rows don't affect this.
DO $$
BEGIN
  IF current_setting('duos.showcase_fixture_target', true) IS DISTINCT FROM 'localdb' THEN
    RAISE EXCEPTION 'showcase fixture: run scripts/seed-showcase-fixture.sh, which targets the local database only';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'dataset_alias_allocate') THEN
    RAISE EXCEPTION 'showcase fixture: the dataset_alias_allocate trigger would replace the fixture aliases; run the Consent migrations first';
  END IF;
  IF EXISTS (
    SELECT 1
    FROM (VALUES ('users', 'user_id'), ('dac', 'dac_id'), ('study', 'study_id'),
                 ('dataset', 'dataset_id'), ('dataset_property', 'property_id')) AS t(tbl, col)
    WHERE COALESCE(pg_sequence_last_value(pg_get_serial_sequence(t.tbl, t.col)::regclass), 0) >= 990001
  ) THEN
    RAISE EXCEPTION 'showcase fixture: the app has issued IDs from 990001, which the fixture uses, so nothing was seeded';
  END IF;
END
$$;

-- Owner of the fixture rows. It never signs in: the address is in the reserved .invalid domain.
INSERT INTO users (user_id, email, display_name, create_date)
VALUES (990001, 'showcase-fixture@duos.invalid', 'Showcase Fixture', now())
ON CONFLICT (user_id) DO UPDATE
  SET email = EXCLUDED.email, display_name = EXCLUDED.display_name;

INSERT INTO dac (dac_id, name, description, create_date, deleted)
VALUES (990001, 'Showcase Fixture DAC', 'Synthetic DAC for local showcase request flows', now(), false)
ON CONFLICT (dac_id) DO UPDATE
  SET name = EXCLUDED.name, description = EXCLUDED.description, deleted = false,
      delete_user_id = NULL, delete_date = NULL;

INSERT INTO study (study_id, name, description, data_types, pi_name, public_visibility, uuid,
                   create_user_id, create_date)
VALUES
  (990001, 'Showcase Fixture Public Study', 'Synthetic study for local showcase request flows',
   ARRAY['Genome'], 'Synthetic PI', true, '00000000-0000-4000-8000-000000990001', 990001, now()),
  (990002, 'Showcase Fixture Private Study', 'Synthetic study that the showcase lookup must not return',
   ARRAY['Genome'], 'Synthetic PI', false, '00000000-0000-4000-8000-000000990002', 990001, now())
ON CONFLICT (study_id) DO UPDATE
  SET name = EXCLUDED.name, description = EXCLUDED.description, data_types = EXCLUDED.data_types,
      pi_name = EXCLUDED.pi_name, public_visibility = EXCLUDED.public_visibility;

-- create_date and dac_approval_date are refreshed on each run, so the fixture leads the
-- "newest" ranking after a reseed.
INSERT INTO dataset (dataset_id, alias, name, study_id, dac_id, dac_approval, dac_approval_date,
                     data_use, create_date, create_user_id)
VALUES
  (990001, 990001, 'Showcase Fixture Controlled Cohort', 990001, 990001, true, now(),
   '{"generalUse":true}', now(), 990001),
  (990002, 990002, 'Showcase Fixture Open Cohort', 990001, 990001, false, NULL,
   '{"generalUse":true}', now(), 990001),
  (990003, 990003, 'Showcase Fixture External Cohort', 990001, 990001, false, NULL,
   '{"generalUse":true}', now(), 990001),
  (990004, 990004, 'Showcase Fixture Unapproved Cohort', 990001, 990001, false, NULL,
   '{"generalUse":true}', now(), 990001),
  (990005, 990005, 'Showcase Fixture Private Cohort', 990002, 990001, true, now(),
   '{"generalUse":true}', now(), 990001)
ON CONFLICT (dataset_id) DO UPDATE
  SET alias = EXCLUDED.alias, name = EXCLUDED.name, study_id = EXCLUDED.study_id,
      dac_id = EXCLUDED.dac_id, dac_approval = EXCLUDED.dac_approval,
      dac_approval_date = EXCLUDED.dac_approval_date, data_use = EXCLUDED.data_use,
      create_date = EXCLUDED.create_date;

-- Catalog entries, as AssetCatalogDAO.ensureDatasetEntries gives a registered dataset: relink an
-- entry the dataset held before (after a delete and a reseed), then add any that is missing. A
-- database restored from a dump older than the catalog has no asset_catalog table yet; the
-- changeset that creates it fills it for existing datasets, so this step is skipped there. If an
-- entry for another dataset already holds a fixture alias, the app would log a collision and leave
-- the dataset without an entry; the seed stops instead, so it never loads a partial fixture.
DO $$
BEGIN
  IF to_regclass('asset_catalog') IS NOT NULL THEN
    UPDATE asset_catalog c SET dataset_id = d.dataset_id, asset_removed_date = NULL
    FROM dataset d
    WHERE d.dataset_id BETWEEN 990001 AND 990005
      AND c.asset_type = 'DATASET'
      AND c.asset_number = d.alias
      AND c.dataset_id IS NULL
      AND c.source_dataset_id = d.dataset_id
      AND NOT EXISTS (SELECT 1 FROM asset_catalog e WHERE e.dataset_id = d.dataset_id);

    INSERT INTO asset_catalog (asset_type, asset_number, dataset_id, source_dataset_id)
    SELECT 'DATASET', d.alias::integer, d.dataset_id, d.dataset_id
    FROM dataset d
    WHERE d.dataset_id BETWEEN 990001 AND 990005
      AND NOT EXISTS (SELECT 1 FROM asset_catalog c WHERE c.dataset_id = d.dataset_id)
    ON CONFLICT (asset_type, asset_number) DO NOTHING;

    -- AssetCatalogDAO.findDatasetCollisions, for the fixture datasets. No DECLARE block: the Jdbi
    -- script parser that the test uses keeps a DO body whole only when it opens with BEGIN.
    IF EXISTS (
      SELECT 1
      FROM dataset d
      JOIN asset_catalog c ON c.asset_type = 'DATASET' AND c.asset_number = d.alias
      WHERE d.dataset_id BETWEEN 990001 AND 990005
        AND c.dataset_id IS DISTINCT FROM d.dataset_id
        AND NOT EXISTS (SELECT 1 FROM asset_catalog e WHERE e.dataset_id = d.dataset_id)
    ) THEN
      RAISE EXCEPTION 'showcase fixture: another asset catalog entry holds %, so nothing was seeded',
        (SELECT string_agg('DUOS-' || d.alias, ', ' ORDER BY d.alias)
         FROM dataset d
         JOIN asset_catalog c ON c.asset_type = 'DATASET' AND c.asset_number = d.alias
         WHERE d.dataset_id BETWEEN 990001 AND 990005
           AND c.dataset_id IS DISTINCT FROM d.dataset_id
           AND NOT EXISTS (SELECT 1 FROM asset_catalog e WHERE e.dataset_id = d.dataset_id));
    END IF;
  END IF;
END
$$;

-- Move the alias sequence past the fixture's aliases, as AssetCatalogDAO.advanceAliasSequencePast
-- does for an alias set by hand, so the sequence never issues one of them to a new dataset. The
-- table lock is the app's: no insert takes an alias between the check and setval. A dump older than
-- the sequence (2026-08-10 changeset) has none; the changeset starts it past the highest alias.
LOCK TABLE dataset IN SHARE ROW EXCLUSIVE MODE;
DO $$
BEGIN
  IF to_regclass('dataset_alias_seq') IS NOT NULL THEN
    PERFORM setval('dataset_alias_seq', 990005)
    FROM dataset_alias_seq s
    WHERE (CASE WHEN s.is_called THEN s.last_value + 1 ELSE s.last_value END) <= 990005;
  END IF;
END
$$;

-- A dictionary key exists once a dataset with that property has been registered. Add a missing one
-- as the app does (DatasetServiceDAO.createDictionaryInsert). There is no unique key on `key`, so
-- check first.
INSERT INTO dictionary (key, required)
SELECT k.key, false
FROM (VALUES ('Access Management'), ('# of participants'), ('URL')) AS k(key)
WHERE NOT EXISTS (SELECT 1 FROM dictionary d WHERE LOWER(d.key) = LOWER(k.key));

-- Editing a dataset in the app deletes a property row and inserts a new one from the sequence, below
-- this range. Remove those rows from the fixture's datasets, so a reseed restores the fixture's
-- own values. Only dataset properties go; data access requests are untouched.
DELETE FROM dataset_property
WHERE dataset_id BETWEEN 990001 AND 990005
  AND property_id NOT BETWEEN 990001 AND 990009;

INSERT INTO dataset_property (property_id, dataset_id, property_key, schema_property, property_type,
                              property_value, create_date)
SELECT p.property_id, p.dataset_id,
       -- Duplicate keys are possible: take the lowest, as DatasetServiceDAO.createPropertyInsert does.
       (SELECT key_id FROM dictionary WHERE LOWER(key) = LOWER(p.dictionary_key) ORDER BY key_id LIMIT 1),
       p.schema_property, p.property_type, p.property_value, now()
FROM (VALUES
  (990001, 990001, 'Access Management', 'accessManagement', 'string', 'controlled'),
  (990002, 990001, '# of participants', 'numberOfParticipants', 'number', '1200'),
  (990003, 990002, 'Access Management', 'accessManagement', 'string', 'open'),
  (990004, 990002, '# of participants', 'numberOfParticipants', 'number', '300'),
  (990005, 990002, 'URL', 'url', 'string', 'https://example.org/showcase-fixture/open'),
  (990006, 990003, 'Access Management', 'accessManagement', 'string', 'external'),
  (990007, 990003, 'URL', 'url', 'string', 'https://example.org/showcase-fixture/external'),
  (990008, 990004, 'Access Management', 'accessManagement', 'string', 'controlled'),
  (990009, 990005, 'Access Management', 'accessManagement', 'string', 'controlled')
) AS p(property_id, dataset_id, dictionary_key, schema_property, property_type, property_value)
ON CONFLICT (property_id) DO UPDATE
  SET dataset_id = EXCLUDED.dataset_id, property_key = EXCLUDED.property_key,
      schema_property = EXCLUDED.schema_property, property_type = EXCLUDED.property_type,
      property_value = EXCLUDED.property_value;

COMMIT;
