package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.TermDarCount;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/** Admin reporting on the ontology terms DARs cite in their research use statements. */
public interface DarTermMetricsDAO {

  /**
   * The terms cited by the most original DARs submitted in [:from, :to), excluding canceled and
   * archived DARs, each DAR counted once per term. Ids match as the ontology reconciliation query
   * does, with a CURIE or OBO IRI also matching the underscored OBO id the importer stores.
   */
  @RegisterConstructorMapper(TermDarCount.class)
  @SqlQuery(
      """
      WITH original_dars AS (
        SELECT dar.reference_id, dar.data
        FROM data_access_request dar
        WHERE dar.parent_id IS NULL
          AND dar.submission_date >= :from AND dar.submission_date < :to
          AND (dar.data->>'status' IS NULL
               OR LOWER(dar.data->>'status') NOT IN ('canceled', 'archived'))
          AND jsonb_typeof(dar.data -> 'ontologies') = 'array'
      ),
      cited AS (
        SELECT od.reference_id,
               TRIM(term ->> 'id') AS term_id,
               LOWER(TRIM(term ->> 'id')) AS norm_id,
               REPLACE(REGEXP_REPLACE(LOWER(TRIM(term ->> 'id')),
                                      '^https?://purl[.]obolibrary[.]org/obo/', ''),
                       ':', '_') AS unindexed_key,
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
        SELECT DISTINCT ON (c.reference_id, COALESCE(LOWER(i.id), c.unindexed_key))
               c.reference_id,
               COALESCE(LOWER(i.id), c.unindexed_key) AS term_key,
               c.term_id,
               c.label,
               i.id AS indexed_id,
               CASE WHEN i.usable THEN i.label END AS indexed_label
        FROM cited c
        LEFT JOIN indexed i ON i.norm_id = c.norm_id
        ORDER BY c.reference_id, COALESCE(LOWER(i.id), c.unindexed_key), c.term_id
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
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("limit") int limit);
}
