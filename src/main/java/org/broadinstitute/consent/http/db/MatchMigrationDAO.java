package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationPopulation;
import org.broadinstitute.consent.http.models.matchmigration.SnapshotReconciliation;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * Backs the one-off migration that replaces {@code match_entity.consent} with a real {@code
 * dataset_id}. Retired with the rest of the migration surface once the constraints are on.
 *
 * <p>The affected set is defined by what the constraints will reject rather than by algorithm
 * version alone: a row needs reprocessing if it has no {@code dataset_id}, or still carries a
 * {@code v1} or {@code v2} stamp, or has no stamp at all. Versions are reported separately because
 * the acceptance criteria count those populations, but they are not what selects the work.
 *
 * <p>{@code ARCHIVED_TEST} repeats the archived-status condition from {@code
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

  String ARCHIVED_TEST =
      "(LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)";

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
          + ARCHIVED_TEST
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

  /**
   * The purposes to reprocess, restricted to those whose DAR still resolves. The rest are reported
   * by {@link #findUnresolvablePurposes()} rather than silently dropped: reprocessing one would
   * delete its matches and insert nothing, because the rebuild has no DAR to read.
   */
  @SqlQuery(
      """
      SELECT DISTINCT m.purpose
      FROM match_entity m
      JOIN data_access_request dar ON dar.reference_id = m.purpose
      WHERE """
          + AFFECTED_PREDICATE
          + " AND "
          + ARCHIVED_TEST
          + """
      ORDER BY m.purpose
      """)
  List<String> findResolvablePurposes();

  /** Affected purposes whose DAR is archived or gone; left untouched and reported for follow-up. */
  @SqlQuery(
      """
      SELECT DISTINCT m.purpose
      FROM match_entity m
      WHERE """
          + AFFECTED_PREDICATE
          + """
        AND NOT EXISTS (
          SELECT 1 FROM data_access_request dar
          WHERE dar.reference_id = m.purpose
            AND """
          + ARCHIVED_TEST
          + """
        )
      ORDER BY m.purpose
      """)
  List<String> findUnresolvablePurposes();

  /**
   * Captures every affected row before anything is reprocessed.
   *
   * <p>{@code ON CONFLICT DO NOTHING} keeps the first capture authoritative: a re-run after a
   * partial failure must not overwrite pre-migration state with post-migration state, and must not
   * fail on the rows it already holds.
   */
  @SqlUpdate(
      """
      INSERT INTO match_migration_snapshot
        (match_id, consent, dataset_id, purpose, match_entity, abstain, failed, create_date,
         algorithm_version)
      SELECT m.match_id, m.consent, m.dataset_id, m.purpose, m.match_entity, m.abstain, m.failed,
             m.create_date, m.algorithm_version
      FROM match_entity m
      WHERE """
          + AFFECTED_PREDICATE
          + """
      ON CONFLICT (match_id) DO NOTHING
      """)
  int snapshotAffectedMatches();

  /**
   * Keyed on the snapshot rather than on {@code match_entity}, so it can never capture rationales
   * for a match the other half did not capture. Re-runnable for the same reason as that half.
   */
  @SqlUpdate(
      """
      INSERT INTO match_migration_rationale_snapshot (match_id, rationale)
      SELECT r.match_entity_id, r.rationale
      FROM match_rationale r
      JOIN match_migration_snapshot s ON s.match_id = r.match_entity_id
      WHERE NOT EXISTS (
        SELECT 1 FROM match_migration_rationale_snapshot existing
        WHERE existing.match_id = r.match_entity_id
          AND existing.rationale = r.rationale
      )
      """)
  int snapshotAffectedRationales();

  /**
   * Measures the snapshot against what the run left behind. A reprocess deletes and re-inserts, so
   * replacements carry new ids and a snapshotted row that is gone was handled.
   */
  @RegisterConstructorMapper(SnapshotReconciliation.class)
  @SqlQuery(
      """
      WITH unresolvable AS (
        SELECT m.match_id
        FROM match_entity m
        WHERE """
          + AFFECTED_PREDICATE
          + """
          AND NOT EXISTS (
            SELECT 1 FROM data_access_request dar
            WHERE dar.reference_id = m.purpose
              AND """
          + ARCHIVED_TEST
          + """
          )
      ),
      counts AS (
        SELECT
        (SELECT COUNT(*) FROM match_migration_snapshot) AS snapshotted,
        (SELECT COUNT(*) FROM match_migration_snapshot s
         WHERE NOT EXISTS (SELECT 1 FROM match_entity m WHERE m.match_id = s.match_id))
          AS snapshotted_rows_gone,
        (SELECT COUNT(*) FROM match_migration_snapshot s
         WHERE EXISTS (SELECT 1 FROM match_entity m WHERE m.match_id = s.match_id))
          AS snapshotted_rows_remaining,
        (SELECT COUNT(*) FROM unresolvable) AS unresolvable_rows,
        (SELECT COUNT(DISTINCT purpose) FROM match_migration_snapshot) AS snapshotted_purposes,
        (SELECT COUNT(*) FROM (
          SELECT DISTINCT s.purpose FROM match_migration_snapshot s
          WHERE EXISTS (SELECT 1 FROM match_entity m WHERE m.purpose = s.purpose)
        ) holding) AS purposes_holding_matches,
        (SELECT COUNT(*) FROM (
          SELECT DISTINCT s.purpose FROM match_migration_snapshot s
          WHERE NOT EXISTS (SELECT 1 FROM match_entity m WHERE m.purpose = s.purpose)
        ) empty) AS purposes_holding_no_matches,
        (SELECT COUNT(*) FROM match_entity m WHERE """
          + AFFECTED_PREDICATE
          + """
        ) AS still_affected
      )
      SELECT counts.*,
             (counts.snapshotted_rows_remaining = counts.unresolvable_rows
               AND counts.still_affected = counts.unresolvable_rows) AS reconciles
      FROM counts
      """)
  SnapshotReconciliation reconcile();
}
