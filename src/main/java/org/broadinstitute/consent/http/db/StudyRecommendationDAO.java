package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.models.StudyRecommendation;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface StudyRecommendationDAO {

  /**
   * The study card fields for the studies in a {@code ranked (study_id, score)} CTE, which each
   * query below defines and this continues. Every field is derived the way ElasticSearchService
   * indexes it, so a card matches the same study on the Studies tab; like that tab, every dataset
   * of a study counts, whatever its DAC approval.
   *
   * <p>A dataset contributes its first "# of participants" property when that parses as an integer,
   * its access management value (the canonical schema property ahead of the legacy consent-group
   * one) and the primary DUO codes TranslationUtil.translateSummary derives from its data use. As
   * there, DS needs a disease restriction the ontology index can label.
   *
   * <p>Model and workspace counts follow StudyAssets.findAssetList: the first promoted property for
   * the type that holds a list is authoritative, even an empty one, and only a study with none
   * falls back to its legacy assets object. Both names are matched ignoring case, as StudyAssets
   * does, because the promotion migration moved only an exactly-cased assets.models and so on; an
   * Assets row or a Models key still holds its list in the legacy object. Should one legacy object
   * hold a key under two casings, jsonb keeps no source order to pick the first by, so the lowest
   * spelling wins. Those rows are cut down in a MATERIALIZED CTE before any JSON cast, because
   * String-typed study properties are bare text and the cast would fail on them if the planner
   * evaluated it ahead of the key filter.
   *
   * <p>Data use and asset values are text columns, so each is cast only once pg_input_is_valid
   * accepts it, inside a CASE so the planner cannot run the cast first. An empty or malformed value
   * then reads as absent, as DataUseParser and StudyAssets treat it, rather than failing the query
   * and with it every recommendation on the list.
   *
   * <p>One known difference from the index: DataUseParser also rejects a whole data use whose JSON
   * is valid but holds a field of the wrong type, such as an object for {@code other}, and the
   * index then shows no codes for it, where this still derives codes from the fields that do match.
   * The application only writes data use by serializing a DataUse, so only a hand-edited row can
   * reach that state, and matching Gson's field typing in SQL would cost a check on every field.
   */
  String CARD_FIELDS =
      """
      , card_datasets AS (
        SELECT d.dataset_id, d.study_id,
          (
            SELECT CASE WHEN dp.property_value ~ '^[+-]?[0-9]+$' THEN
              CASE WHEN dp.property_value::numeric BETWEEN -2147483648 AND 2147483647
                THEN dp.property_value::int END
            END
            FROM dataset_property dp
            INNER JOIN dictionary k ON k.key_id = dp.property_key
            WHERE dp.dataset_id = d.dataset_id AND LOWER(k.key) = '# of participants'
            ORDER BY dp.property_id
            LIMIT 1
          ) AS participant_count,
          (
            SELECT LOWER(TRIM(dp.property_value))
            FROM dataset_property dp
            WHERE dp.dataset_id = d.dataset_id
              AND LOWER(dp.schema_property) IN ('accessmanagement', 'consentgroup.accessmanagement')
              AND LOWER(TRIM(dp.property_value)) IN ('open', 'controlled', 'external')
            ORDER BY LOWER(dp.schema_property) = 'consentgroup.accessmanagement', dp.property_id
            LIMIT 1
          ) AS access_type,
          ARRAY_REMOVE(ARRAY[
            CASE WHEN LOWER(u.du ->> 'generalUse') = 'true' THEN 'GRU' END,
            CASE WHEN jsonb_typeof(u.du -> 'diseaseRestrictions') = 'array' THEN
              CASE WHEN EXISTS (
                SELECT 1
                FROM jsonb_array_elements_text(u.du -> 'diseaseRestrictions') AS t(term_id)
                INNER JOIN ontology_index oi
                  ON LOWER(oi.id) = LOWER(TRIM(t.term_id)) OR LOWER(oi.obo_id) = LOWER(TRIM(t.term_id))
                WHERE oi.json_document ->> 'label' ~ '\\S'
              ) THEN 'DS' END
            END,
            CASE WHEN LOWER(u.du ->> 'hmbResearch') = 'true' THEN 'HMB' END,
            CASE WHEN LOWER(u.du ->> 'populationOriginsAncestry') = 'true' THEN 'NPOA' END,
            CASE WHEN u.du ->> 'other' ~ '\\S' THEN 'OTHER' END
          ], NULL) AS data_use_codes
        FROM dataset d
        CROSS JOIN LATERAL (
          SELECT CASE WHEN pg_input_is_valid(d.data_use, 'jsonb') THEN d.data_use::jsonb END AS du
        ) u
        WHERE d.study_id IN (SELECT study_id FROM ranked)
      ), card_dataset_totals AS (
        SELECT cd.study_id, COUNT(*) AS dataset_count,
          ARRAY_AGG(DISTINCT cd.dataset_id) AS dataset_ids,
          SUM(cd.participant_count) AS total_participants,
          ARRAY_REMOVE(ARRAY_AGG(DISTINCT cd.access_type), NULL) AS access_types
        FROM card_datasets cd
        GROUP BY cd.study_id
      ), card_data_use_codes AS (
        SELECT cd.study_id, ARRAY_AGG(DISTINCT code) AS data_use_codes
        FROM card_datasets cd
        CROSS JOIN LATERAL UNNEST(cd.data_use_codes) AS code
        GROUP BY cd.study_id
      ), card_assets AS MATERIALIZED (
        SELECT sp.study_property_id, sp.study_id, LOWER(sp.key) AS key,
          CASE WHEN pg_input_is_valid(sp.value, 'jsonb') THEN sp.value::jsonb END AS value
        FROM study_property sp
        WHERE sp.study_id IN (SELECT study_id FROM ranked)
          AND LOWER(sp.key) IN ('models', 'workspaces', 'assets')
      ), card_promoted_assets AS (
        -- The first promoted row per type that holds a list, as StudyAssets reads it
        SELECT DISTINCT ON (ca.study_id, ca.key) ca.study_id, ca.key,
          jsonb_array_length(ca.value) AS asset_count
        FROM card_assets ca
        WHERE ca.key IN ('models', 'workspaces') AND jsonb_typeof(ca.value) = 'array'
        ORDER BY ca.study_id, ca.key, ca.study_property_id
      ), card_legacy_assets AS (
        -- The first legacy object with anything in it, as StudyAssets.findLegacyAssets reads it
        SELECT DISTINCT ON (ca.study_id) ca.study_id, ca.value
        FROM card_assets ca
        WHERE ca.key = 'assets' AND jsonb_typeof(ca.value) = 'object' AND ca.value != '{}'::jsonb
        ORDER BY ca.study_id, ca.study_property_id
      ), card_legacy_asset_counts AS (
        -- A legacy value that is not a list counts nothing, as findAssetList reads it
        SELECT DISTINCT ON (la.study_id, LOWER(kv.key)) la.study_id, LOWER(kv.key) AS key,
          CASE WHEN jsonb_typeof(kv.value) = 'array' THEN jsonb_array_length(kv.value) ELSE 0 END
            AS asset_count
        FROM card_legacy_assets la
        CROSS JOIN LATERAL jsonb_each(la.value) AS kv
        WHERE LOWER(kv.key) IN ('models', 'workspaces')
        ORDER BY la.study_id, LOWER(kv.key), kv.key
      ), card_asset_counts AS (
        SELECT pa.study_id, pa.key, pa.asset_count FROM card_promoted_assets pa
        UNION ALL
        SELECT la.study_id, la.key, la.asset_count
        FROM card_legacy_asset_counts la
        WHERE NOT EXISTS (
          SELECT 1 FROM card_promoted_assets pa
          WHERE pa.study_id = la.study_id AND pa.key = la.key
        )
      )
      SELECT s.study_id, s.name AS study_name, s.description AS study_description, s.pi_name,
        (SELECT sp.value FROM study_property sp WHERE sp.study_id = s.study_id AND sp.key = 'species'
          ORDER BY sp.study_property_id LIMIT 1) AS species,
        (SELECT sp.value FROM study_property sp
          WHERE sp.study_id = s.study_id AND sp.key = 'phenotypeIndication'
          ORDER BY sp.study_property_id LIMIT 1) AS phenotype,
        COALESCE(s.data_types, ARRAY[]::text[]) AS data_types,
        COALESCE(dt.dataset_count, 0) AS dataset_count,
        COALESCE(dt.dataset_ids, ARRAY[]::int[]) AS dataset_ids,
        COALESCE(dt.total_participants, 0) AS total_participants,
        COALESCE(models.asset_count, 0) AS model_count,
        COALESCE(workspaces.asset_count, 0) AS workspace_count,
        COALESCE(dt.access_types, ARRAY[]::text[]) AS access_types,
        COALESCE(duc.data_use_codes, ARRAY[]::text[]) AS data_use_codes
      FROM ranked r
      INNER JOIN study s ON s.study_id = r.study_id
      LEFT JOIN card_dataset_totals dt ON dt.study_id = s.study_id
      LEFT JOIN card_data_use_codes duc ON duc.study_id = s.study_id
      LEFT JOIN card_asset_counts models ON models.study_id = s.study_id AND models.key = 'models'
      LEFT JOIN card_asset_counts workspaces
        ON workspaces.study_id = s.study_id AND workspaces.key = 'workspaces'
      ORDER BY r.score DESC, r.study_id
      """;

  /**
   * Publicly visible studies that share the source study's PI or at least one data type, best
   * matches first. A blank pi_name is not an identity, so it never matches another blank one.
   */
  @RegisterConstructorMapper(StudyRecommendation.class)
  @SqlQuery(
      """
      WITH source AS (SELECT data_types, NULLIF(pi_name, '') AS pi_name FROM study WHERE study_id = :studyId),
      ranked AS (
        SELECT s.study_id, (
          SELECT COUNT(*) FROM (
            SELECT UNNEST(COALESCE(s.data_types, ARRAY[]::text[]))
            INTERSECT SELECT UNNEST(COALESCE(source.data_types, ARRAY[]::text[]))
          ) overlap
        ) + CASE WHEN NULLIF(s.pi_name, '') = source.pi_name THEN 1 ELSE 0 END AS score
        FROM study s
        CROSS JOIN source
        WHERE s.study_id <> :studyId AND s.public_visibility = TRUE
          AND (NULLIF(s.pi_name, '') = source.pi_name OR s.data_types && source.data_types)
        ORDER BY score DESC, s.study_id
        LIMIT 12
      )
      """
          + CARD_FIELDS)
  List<StudyRecommendation> findSimilar(@Bind("studyId") Integer studyId);

  /**
   * Publicly visible studies most often requested in the same data access request as the source
   * study. Only submitted, non-archived, non-progress-report DARs count towards a score: a draft
   * cart is not a request, an archived DAR should stop counting, and a progress report carries its
   * own reference_id, so counting one would score its parent DAR more than once.
   */
  @RegisterConstructorMapper(StudyRecommendation.class)
  @SqlQuery(
      """
      WITH source_references AS (
        SELECT DISTINCT dd.reference_id
        FROM data_access_request dar
        INNER JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
        INNER JOIN dataset d ON d.dataset_id = dd.dataset_id
        WHERE d.study_id = :studyId
          AND dar.submission_date IS NOT NULL
          AND dar.parent_id IS NULL
          AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
      ), candidate_scores AS (
        -- A reference_id in source_references is by construction a qualifying DAR, so no
        -- second pass over data_access_request is needed here.
        SELECT d.study_id, COUNT(DISTINCT dd.reference_id) AS score
        FROM source_references sr
        INNER JOIN dar_dataset dd ON dd.reference_id = sr.reference_id
        INNER JOIN dataset d ON d.dataset_id = dd.dataset_id
        WHERE d.study_id <> :studyId
        GROUP BY d.study_id
      ), ranked AS (
        SELECT cs.study_id, cs.score
        FROM candidate_scores cs
        INNER JOIN study s ON s.study_id = cs.study_id AND s.public_visibility = TRUE
        ORDER BY cs.score DESC, cs.study_id
        LIMIT 12
      )
      """
          + CARD_FIELDS)
  List<StudyRecommendation> findFrequentlyRequestedWith(@Bind("studyId") Integer studyId);
}
