package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.TermDarCount;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/** Reporting on the ontology terms DARs cite in their research use statements. */
public interface DarTermMetricsDAO {

  /**
   * The terms cited by the most DAR submissions in [:from, :to), excluding canceled and archived
   * DARs, each submission counted once per term. Before 2022-07-27 a submission was saved as one
   * DAR per dataset, so a collection's original DARs count as one. Ids match as the ontology
   * reconciliation query does, with a CURIE or OBO IRI also matching the underscored OBO id the
   * importer stores. Only DARs requesting a dataset now in :dacIds are read, or every DAR when it's
   * null.
   */
  @RegisterConstructorMapper(TermDarCount.class)
  @SqlQuery(
      """
      WITH original_dars AS (
        SELECT COALESCE(dar.collection_id::text, dar.reference_id) AS submission_key, dar.data
        FROM data_access_request dar
        WHERE dar.parent_id IS NULL
          AND dar.submission_date >= :from AND dar.submission_date < :to
          AND (dar.data->>'status' IS NULL
               OR LOWER(dar.data->>'status') NOT IN ('canceled', 'archived'))
          AND jsonb_typeof(dar.data -> 'ontologies') = 'array'
          AND (CAST(:dacIds AS int[]) IS NULL
               OR EXISTS (SELECT 1 FROM dar_dataset dd
                          WHERE dd.reference_id = dar.reference_id
                            AND dd.dataset_id IN (SELECT dataset_id FROM dataset
                                                  WHERE dac_id = ANY(CAST(:dacIds AS int[])))))
      ),
      cited AS (
        SELECT od.submission_key,
               TRIM(term ->> 'id') AS term_id,
               LOWER(TRIM(term ->> 'id')) AS norm_id,
               CASE
                 WHEN LOWER(TRIM(term ->> 'id')) ~ '^https?://purl[.]obolibrary[.]org/obo/[a-z]+_[a-z0-9]+$'
                   THEN REGEXP_REPLACE(LOWER(TRIM(term ->> 'id')), '^.*/obo/', '')
                 WHEN LOWER(TRIM(term ->> 'id')) ~ '^[a-z]+:[a-z0-9]+$'
                   THEN REPLACE(LOWER(TRIM(term ->> 'id')), ':', '_')
                 ELSE LOWER(TRIM(term ->> 'id'))
               END AS unindexed_key,
               NULLIF(TRIM(term ->> 'label'), '') AS label
        FROM original_dars od
        CROSS JOIN jsonb_array_elements(od.data -> 'ontologies') AS term
        WHERE jsonb_typeof(term) = 'object' AND NULLIF(TRIM(term ->> 'id'), '') IS NOT NULL
      ),
      cited_ids AS (
        SELECT DISTINCT norm_id, unindexed_key FROM cited
      ),
      matches AS (
        SELECT ci.norm_id, oi.id, oi.label, oi.usable, 1 AS preference
        FROM cited_ids ci
        JOIN ontology_index oi ON LOWER(TRIM(oi.id)) = ci.norm_id
        UNION ALL
        SELECT ci.norm_id, oi.id, oi.label, oi.usable, 2 AS preference
        FROM cited_ids ci
        JOIN ontology_index oi ON LOWER(TRIM(oi.obo_id)) = ci.unindexed_key
      ),
      indexed AS (
        SELECT DISTINCT ON (norm_id) norm_id, id, label, usable
        FROM matches
        ORDER BY norm_id, preference, id
      ),
      dar_terms AS (
        SELECT DISTINCT ON (c.submission_key, COALESCE(LOWER(i.id), c.unindexed_key))
               c.submission_key,
               COALESCE(LOWER(i.id), c.unindexed_key) AS term_key,
               c.term_id,
               c.label,
               i.id AS indexed_id,
               CASE WHEN i.usable THEN i.label END AS indexed_label
        FROM cited c
        LEFT JOIN indexed i ON i.norm_id = c.norm_id
        ORDER BY c.submission_key, COALESCE(LOWER(i.id), c.unindexed_key), c.term_id
      )
      SELECT COALESCE(MIN(indexed_id), MODE() WITHIN GROUP (ORDER BY term_id)) AS id,
             COALESCE(MIN(indexed_label), MODE() WITHIN GROUP (ORDER BY label)) AS label,
             COUNT(*) AS dar_count
      FROM dar_terms
      GROUP BY term_key
      ORDER BY dar_count DESC, label, id
      LIMIT :limit
      """)
  List<TermDarCount> findTopTerms(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("dacIds") List<Integer> dacIds,
      @Bind("limit") int limit);
}
