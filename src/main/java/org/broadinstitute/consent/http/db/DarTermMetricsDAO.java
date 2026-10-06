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
   * The terms cited by the most original DARs submitted in [:from, :to), most first, leaving out
   * canceled and archived DARs. A cited id matches an indexed term's id or OBO id, trimmed and
   * case-insensitively, as OntologyDAO's lookups do, and a DAR counts once per term however often
   * it cites it. The id and label are the indexed term's, the label only if the term is usable;
   * otherwise they're the ones the most DARs recorded.
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
               NULLIF(TRIM(term ->> 'label'), '') AS label
        FROM original_dars od
        CROSS JOIN jsonb_array_elements(od.data -> 'ontologies') AS term
        WHERE jsonb_typeof(term) = 'object' AND NULLIF(TRIM(term ->> 'id'), '') IS NOT NULL
      ),
      dar_terms AS (
        SELECT DISTINCT ON (c.reference_id, COALESCE(LOWER(oi.id), c.norm_id))
               c.reference_id,
               COALESCE(LOWER(oi.id), c.norm_id) AS term_key,
               c.term_id,
               c.label,
               oi.id AS indexed_id,
               CASE WHEN oi.usable THEN oi.label END AS indexed_label
        FROM cited c
        LEFT JOIN ontology_index oi
          ON LOWER(TRIM(oi.id)) = c.norm_id OR LOWER(TRIM(oi.obo_id)) = c.norm_id
        ORDER BY c.reference_id, COALESCE(LOWER(oi.id), c.norm_id), c.term_id
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
