package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.TurnaroundBucket;
import org.broadinstitute.consent.http.models.VolumeBucketCount;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * The DAR trend reports on a study's or dataset's page: per-bucket totals over the DARs that
 * request the datasets in :datasetIds, counting only those datasets. An empty :datasetIds reads
 * nothing.
 *
 * <p>These pages are open to anyone who can read the study or dataset, so every query returns
 * counts alone: no DAR, collection, researcher or institution is identified, and there are no rows
 * to page.
 *
 * <p>Each query applies the same rule as the {@link DarMetricsDAO} fragment its javadoc names,
 * which documents the rule in full, with the DAC scope replaced by :datasetIds. A change to one
 * belongs in the other. The queries are written out whole rather than built from those fragments,
 * as docs/ai/CLAUDE.md asks.
 */
public interface ScopedDarMetricsDAO {

  /**
   * Original DARs submitted in [:from, :to) that request a dataset in scope, per submission bucket,
   * with how many datasets in scope they request. As {@link DarMetricsDAO#DAR_VOLUME}: canceled and
   * archived DARs are left out, and a collection's original DARs count as one submission, under its
   * earliest that requests a dataset in scope. Researchers and institutions are not counted, so the
   * report says nothing about who requested the data.
   */
  @RegisterConstructorMapper(VolumeBucketCount.class)
  @SqlQuery(
      """
      WITH scoped_dars AS (
        SELECT COALESCE(dar.collection_id::text, dar.reference_id) AS submission_key,
               dar.submission_date, COUNT(*) AS dataset_count
        FROM data_access_request dar
        JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
        WHERE dar.parent_id IS NULL
          AND dar.submission_date >= :from AND dar.submission_date < :to
          AND (dar.data->>'status' IS NULL
               OR LOWER(dar.data->>'status') NOT IN ('canceled', 'archived'))
          AND dd.dataset_id = ANY(CAST(:datasetIds AS int[]))
        GROUP BY dar.reference_id, dar.collection_id, dar.submission_date
      ),
      submissions AS (
        SELECT MIN(submission_date) AS submission_date, SUM(dataset_count) AS dataset_count
        FROM scoped_dars
        GROUP BY submission_key
      )
      SELECT date_trunc(:bucket, submission_date) AS bucket_start,
             COUNT(*) AS dar_count,
             CAST(NULL AS bigint) AS researcher_count,
             CAST(NULL AS bigint) AS institution_count,
             CAST(SUM(dataset_count) AS bigint) AS dataset_count
      FROM submissions
      GROUP BY 1
      ORDER BY 1
      """)
  List<VolumeBucketCount> countDarVolume(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("datasetIds") List<Integer> datasetIds,
      @Bind("bucket") String bucket);

  /**
   * DAC decisions on the datasets in scope, per DAR-dataset pair on an original DAR submitted in
   * [:from, :to), counted per submission bucket, state and how it was decided. As {@link
   * DarMetricsDAO#PAIR_DECISIONS}: a pair carries the state of its latest data-access election,
   * whose cast final or RADAR vote is the decision; with none cast it is pending, or canceled if a
   * chair canceled the election.
   */
  @RegisterConstructorMapper(DecisionBucketCount.class)
  @SqlQuery(
      """
      WITH scoped_pairs AS (
        SELECT dar.reference_id, dd.dataset_id, dar.submission_date
        FROM data_access_request dar
        JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
        WHERE dar.parent_id IS NULL
          AND dar.submission_date >= :from AND dar.submission_date < :to
          AND (dar.data->>'status' IS NULL
               OR LOWER(dar.data->>'status') NOT IN ('canceled', 'archived'))
          AND dd.dataset_id = ANY(CAST(:datasetIds AS int[]))
      ),
      latest_elections AS (
        SELECT DISTINCT ON (e.reference_id, e.dataset_id)
               e.election_id, e.reference_id, e.dataset_id, e.status
        FROM election e
        JOIN scoped_pairs sp ON sp.reference_id = e.reference_id AND sp.dataset_id = e.dataset_id
        WHERE LOWER(e.election_type) = 'dataaccess'
        ORDER BY e.reference_id, e.dataset_id, e.election_id DESC
      ),
      deciding_votes AS (
        SELECT DISTINCT ON (v.election_id) v.election_id, v.vote, v.type
        FROM vote v
        JOIN latest_elections le ON le.election_id = v.election_id
        WHERE LOWER(v.type) IN ('final', 'radar_approve') AND v.vote IS NOT NULL
        ORDER BY v.election_id, COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC
      ),
      pair_decisions AS (
        SELECT sp.submission_date,
               CASE WHEN dv.vote THEN 'APPROVED'
                    WHEN NOT dv.vote THEN 'DENIED'
                    WHEN le.election_id IS NULL THEN 'NO_ELECTION'
                    WHEN LOWER(le.status) = 'canceled' THEN 'CANCELED'
                    ELSE 'PENDING' END AS state,
               CASE WHEN dv.vote IS NULL THEN NULL
                    WHEN LOWER(dv.type) = 'radar_approve' THEN 'RADAR'
                    ELSE 'MANUAL' END AS decided_via
        FROM scoped_pairs sp
        LEFT JOIN latest_elections le
          ON le.reference_id = sp.reference_id AND le.dataset_id = sp.dataset_id
        LEFT JOIN deciding_votes dv ON dv.election_id = le.election_id
      )
      SELECT date_trunc(:bucket, submission_date) AS bucket_start, state, decided_via,
             COUNT(*) AS count
      FROM pair_decisions
      GROUP BY 1, 2, 3
      ORDER BY 1, 2, 3
      """)
  List<DecisionBucketCount> countPairDecisions(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("datasetIds") List<Integer> datasetIds,
      @Bind("bucket") String bucket);

  /**
   * Time from submission to DAC decision over the decided pairs of {@link #countPairDecisions}, per
   * submission bucket. As {@link DarMetricsDAO#countPairTurnaround}: a decision whose vote has no
   * update date, or one before the submission date, is counted as unmeasured and not timed.
   */
  @RegisterConstructorMapper(TurnaroundBucket.class)
  @SqlQuery(
      """
      WITH scoped_pairs AS (
        SELECT dar.reference_id, dd.dataset_id, dar.submission_date
        FROM data_access_request dar
        JOIN dar_dataset dd ON dd.reference_id = dar.reference_id
        WHERE dar.parent_id IS NULL
          AND dar.submission_date >= :from AND dar.submission_date < :to
          AND (dar.data->>'status' IS NULL
               OR LOWER(dar.data->>'status') NOT IN ('canceled', 'archived'))
          AND dd.dataset_id = ANY(CAST(:datasetIds AS int[]))
      ),
      latest_elections AS (
        SELECT DISTINCT ON (e.reference_id, e.dataset_id)
               e.election_id, e.reference_id, e.dataset_id
        FROM election e
        JOIN scoped_pairs sp ON sp.reference_id = e.reference_id AND sp.dataset_id = e.dataset_id
        WHERE LOWER(e.election_type) = 'dataaccess'
        ORDER BY e.reference_id, e.dataset_id, e.election_id DESC
      ),
      deciding_votes AS (
        SELECT DISTINCT ON (v.election_id) v.election_id, v.update_date
        FROM vote v
        JOIN latest_elections le ON le.election_id = v.election_id
        WHERE LOWER(v.type) IN ('final', 'radar_approve') AND v.vote IS NOT NULL
        ORDER BY v.election_id, COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC
      ),
      decided_pairs AS (
        SELECT sp.submission_date,
               CASE WHEN dv.update_date >= sp.submission_date
                    THEN EXTRACT(EPOCH FROM dv.update_date - sp.submission_date)::float8 / 86400
                    END AS elapsed_days
        FROM scoped_pairs sp
        JOIN latest_elections le
          ON le.reference_id = sp.reference_id AND le.dataset_id = sp.dataset_id
        JOIN deciding_votes dv ON dv.election_id = le.election_id
      )
      SELECT date_trunc(:bucket, submission_date) AS bucket_start,
             COUNT(elapsed_days) AS count,
             COUNT(*) - COUNT(elapsed_days) AS unmeasured,
             AVG(elapsed_days) AS mean_days,
             PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY elapsed_days) AS median_days,
             (MODE() WITHIN GROUP (ORDER BY FLOOR(elapsed_days)))::int AS mode_days
      FROM decided_pairs
      GROUP BY 1
      ORDER BY 1
      """)
  List<TurnaroundBucket> countPairTurnaround(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("datasetIds") List<Integer> datasetIds,
      @Bind("bucket") String bucket);
}
