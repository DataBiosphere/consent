package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.ExpirationBucket;
import org.broadinstitute.consent.http.models.RenewalBucket;
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
 * belongs in the other; {@code ScopedDarMetricsDAOTest} holds the two to the same totals. The
 * queries are written out whole rather than built from those fragments, as docs/ai/CLAUDE.md asks.
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

  /**
   * DAR collections whose access to the datasets in scope ended in [:from, :to), and by :asOf, per
   * access-end bucket and reason. As {@link DarMetricsDAO#EXPIRATIONS}: access to a dataset runs
   * 8760 hours from the newest submission, original or progress report, whose most recently cast
   * final or RADAR vote approved it; a closeout filed before the term runs out ends the whole
   * collection's access on its filing date; and a collection has ended once access to every dataset
   * in scope has, however its datasets outside the scope stand.
   */
  @RegisterConstructorMapper(ExpirationBucket.class)
  @SqlQuery(
      """
      WITH ranked_dars AS (
        SELECT dar.reference_id, dar.collection_id, dar.submission_date
        FROM data_access_request dar
        WHERE dar.submission_date >= CAST(:from AS timestamp) - INTERVAL '366 days'
          AND (dar.parent_id IS NULL OR dar.data->>'closeoutSupplement' IS NULL)
          AND (dar.data->>'status' IS NULL OR LOWER(dar.data->>'status') != 'archived')
      ),
      last_votes AS (
        SELECT DISTINCT ON (e.reference_id, e.dataset_id)
               rd.collection_id, e.dataset_id, rd.submission_date, v.vote
        FROM ranked_dars rd
        JOIN dar_dataset dd ON dd.reference_id = rd.reference_id
        JOIN election e
          ON e.reference_id = dd.reference_id AND e.dataset_id = dd.dataset_id
         AND LOWER(e.election_type) = 'dataaccess'
        JOIN vote v
          ON v.election_id = e.election_id AND v.vote IS NOT NULL
         AND LOWER(v.type) IN ('final', 'radar_approve')
        WHERE dd.dataset_id = ANY(CAST(:datasetIds AS int[]))
        ORDER BY e.reference_id, e.dataset_id,
                 COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC
      ),
      term_ends AS (
        SELECT collection_id,
               MAX(submission_date)::timestamptz + INTERVAL '8760 hours' AS term_end
        FROM last_votes
        WHERE vote
        GROUP BY collection_id, dataset_id
      ),
      collection_ends AS (
        SELECT t.collection_id, MAX(t.term_end) AS term_end,
               (SELECT MAX(dar.submission_date)::timestamptz FROM data_access_request dar
                WHERE dar.collection_id = t.collection_id
                  AND dar.submission_date IS NOT NULL
                  AND dar.parent_id IS NOT NULL
                  AND dar.data->>'closeoutSupplement' IS NOT NULL) AS closeout_date
        FROM term_ends t
        GROUP BY t.collection_id
      ),
      expirations AS (
        SELECT LEAST(ce.term_end, ce.closeout_date) AS access_end,
               CASE WHEN ce.closeout_date < ce.term_end THEN 'CLOSED_OUT' ELSE 'EXPIRED' END
                 AS reason
        FROM collection_ends ce
        -- Selects nothing from it: the join drops legacy DARs with no collection, as the admin
        -- report's does
        JOIN dar_collection dc ON dc.collection_id = ce.collection_id
      )
      SELECT date_trunc(:bucket, access_end) AS bucket_start, reason, COUNT(*) AS count
      FROM expirations
      WHERE access_end >= :from AND access_end < :to AND access_end <= :asOf
      GROUP BY 1, 2
      ORDER BY 1, 2
      """)
  List<ExpirationBucket> countExpirations(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("datasetIds") List<Integer> datasetIds,
      @Bind("asOf") Instant asOf,
      @Bind("bucket") String bucket);

  /**
   * Datasets in scope renewed by progress reports submitted in [:from, :to), and the collections
   * they renewed, per submission bucket. As {@link DarMetricsDAO#RENEWALS}: a renewal is a dataset
   * approved on a progress report by its most recently cast final or RADAR vote; closeouts and
   * canceled or archived reports renew nothing.
   */
  @RegisterConstructorMapper(RenewalBucket.class)
  @SqlQuery(
      """
      WITH progress_reports AS (
        SELECT dar.reference_id, dar.collection_id, dar.submission_date
        FROM data_access_request dar
        WHERE dar.parent_id IS NOT NULL
          AND dar.data->>'closeoutSupplement' IS NULL
          AND dar.submission_date >= :from AND dar.submission_date < :to
          AND (dar.data->>'status' IS NULL
               OR LOWER(dar.data->>'status') NOT IN ('canceled', 'archived'))
      ),
      last_votes AS (
        SELECT DISTINCT ON (e.reference_id, e.dataset_id)
               pr.collection_id, pr.submission_date, v.vote
        FROM progress_reports pr
        JOIN dar_dataset dd ON dd.reference_id = pr.reference_id
        JOIN election e
          ON e.reference_id = dd.reference_id AND e.dataset_id = dd.dataset_id
         AND LOWER(e.election_type) = 'dataaccess'
        JOIN vote v
          ON v.election_id = e.election_id AND v.vote IS NOT NULL
         AND LOWER(v.type) IN ('final', 'radar_approve')
        WHERE dd.dataset_id = ANY(CAST(:datasetIds AS int[]))
        ORDER BY e.reference_id, e.dataset_id,
                 COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC
      )
      SELECT date_trunc(:bucket, submission_date) AS bucket_start,
             COUNT(*) AS renewal_count, COUNT(DISTINCT collection_id) AS collection_count
      FROM last_votes
      WHERE vote
      GROUP BY 1
      ORDER BY 1
      """)
  List<RenewalBucket> countRenewals(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("datasetIds") List<Integer> datasetIds,
      @Bind("bucket") String bucket);
}
