package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationPopulation;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * Backs the one-off migration that replaces {@code match_entity.consent} with a real {@code
 * dataset_id}. Retired with the rest of the migration surface once the constraints are on.
 *
 * <p>The affected set is defined by what the constraints will reject rather than by algorithm
 * version alone: a row needs reprocessing if it has no {@code dataset_id}, or still carries a
 * {@code v1} or {@code v2} stamp, or has no stamp at all. Versions are reported separately because
 * the acceptance criteria count those populations, but they are not what selects the work.
 *
 * <p>{@code NOT_ARCHIVED} repeats the not-archived condition from {@code
 * DataAccessRequestDAO.findByReferenceId} rather than sharing it. That lookup is what a reprocess
 * resolves its DAR through, so the run has to predict it exactly; if it changes, these queries have
 * to be revisited deliberately, not silently follow it.
 */
public interface MatchMigrationDAO {

  String AFFECTED_PREDICATE =
      """
      (m.dataset_id IS NULL
        OR m.algorithm_version IS NULL
        OR LOWER(m.algorithm_version) IN ('v1', 'v2'))
      """;

  String NOT_ARCHIVED = "(LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)";

  /**
   * Counted in one statement so the totals cannot disagree with each other, and recounted on every
   * call because the affected set drains as DARs are re-matched.
   */
  @RegisterConstructorMapper(MatchMigrationPopulation.class)
  @SqlQuery(
      """
      WITH affected AS (
        SELECT m.match_id, m.purpose, m.dataset_id, m.algorithm_version
        FROM match_entity m
        WHERE """
          + AFFECTED_PREDICATE
          + """
      ),
      resolvable AS (
        SELECT DISTINCT a.purpose
        FROM affected a
        JOIN data_access_request dar ON dar.reference_id = a.purpose
        WHERE """
          + NOT_ARCHIVED
          + """
      ),
      counts AS (
        SELECT
        (SELECT COUNT(*) FROM affected) AS affected_matches,
        (SELECT COUNT(DISTINCT purpose) FROM affected) AS affected_purposes,
        (SELECT COUNT(*) FROM affected WHERE LOWER(algorithm_version) = 'v1') AS version_v1,
        (SELECT COUNT(*) FROM affected WHERE LOWER(algorithm_version) = 'v2') AS version_v2,
        (SELECT COUNT(*) FROM affected WHERE algorithm_version IS NULL) AS version_null,
        (SELECT COUNT(*) FROM affected WHERE dataset_id IS NULL) AS missing_dataset_id,
        (SELECT COUNT(*) FROM resolvable) AS resolvable_purposes,
        (SELECT COUNT(DISTINCT purpose) FROM affected) - (SELECT COUNT(*) FROM resolvable)
          AS archived_or_missing_purposes,
        (SELECT COUNT(*) FROM (
          SELECT purpose FROM affected GROUP BY purpose HAVING COUNT(*) > 1
        ) multi) AS purposes_with_multiple_affected_matches,
        (SELECT COUNT(*) FROM (
          SELECT purpose, dataset_id FROM match_entity
          WHERE dataset_id IS NOT NULL
          GROUP BY purpose, dataset_id HAVING COUNT(*) > 1
        ) dupes) AS duplicate_purpose_dataset_pairs
      )
      SELECT counts.*,
             (counts.affected_matches > 0 OR counts.duplicate_purpose_dataset_pairs > 0)
               AS blocks_constraints
      FROM counts
      """)
  MatchMigrationPopulation findPopulation();
}
