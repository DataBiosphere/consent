package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.DarDatasetDecision;
import org.broadinstitute.consent.http.models.DarDecision;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/** Admin reporting over DAR decisions. Every query is bounded by a submission date range. */
public interface DarMetricsDAO {

  /**
   * One row per DAR-dataset pair on an original DAR submitted in [:from, :to), carrying the state
   * of the pair's latest data-access election. A reopen archives the earlier elections and opens a
   * new one, and product counts the reopen as overwriting the earlier decision, so only the latest
   * election is read. Its cast final or RADAR vote is the decision; with none cast the pair is
   * pending, or canceled if a chair canceled the election. Elections are ranked only for DARs in
   * range, so the ranking never sorts the whole table.
   */
  String PAIR_DECISIONS =
      """
      WITH original_dars AS (
        SELECT dar.reference_id, dar.collection_id, dar.submission_date
        FROM data_access_request dar
        WHERE dar.parent_id IS NULL
          AND dar.submission_date >= :from AND dar.submission_date < :to
          AND (dar.data->>'status' IS NULL
               OR LOWER(dar.data->>'status') NOT IN ('canceled', 'archived'))
      ),
      latest_elections AS (
        SELECT DISTINCT ON (e.reference_id, e.dataset_id)
               e.election_id, e.reference_id, e.dataset_id, e.status
        FROM election e
        JOIN original_dars od ON od.reference_id = e.reference_id
        WHERE LOWER(e.election_type) = 'dataaccess'
        ORDER BY e.reference_id, e.dataset_id, e.election_id DESC
      ),
      deciding_votes AS (
        SELECT DISTINCT ON (v.election_id) v.election_id, v.vote, v.type, v.update_date
        FROM vote v
        JOIN latest_elections le ON le.election_id = v.election_id
        WHERE LOWER(v.type) IN ('final', 'radar_approve') AND v.vote IS NOT NULL
        ORDER BY v.election_id, COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC
      ),
      pair_decisions AS (
        SELECT od.reference_id, od.collection_id, dd.dataset_id, od.submission_date,
               CASE WHEN dv.vote THEN 'APPROVED'
                    WHEN NOT dv.vote THEN 'DENIED'
                    WHEN le.election_id IS NULL THEN 'NO_ELECTION'
                    WHEN LOWER(le.status) = 'canceled' THEN 'CANCELED'
                    ELSE 'PENDING' END AS state,
               CASE WHEN dv.vote IS NULL THEN NULL
                    WHEN LOWER(dv.type) = 'radar_approve' THEN 'RADAR'
                    ELSE 'MANUAL' END AS decided_via,
               dv.update_date AS decision_date
        FROM original_dars od
        JOIN dar_dataset dd ON dd.reference_id = od.reference_id
        LEFT JOIN latest_elections le
          ON le.reference_id = dd.reference_id AND le.dataset_id = dd.dataset_id
        LEFT JOIN deciding_votes dv ON dv.election_id = le.election_id
      )
      """;

  /**
   * Rolls pairs up per DAR. A DAR is decided once no pair is pending; canceled pairs don't hold it
   * open or affect its outcome, and one whose pairs were all canceled is canceled. Its decision
   * date is the last pair decision, left null when any deciding vote predates decision dates.
   */
  String DAR_DECISIONS =
      PAIR_DECISIONS
          + """
          , dar_decisions AS (
            SELECT reference_id, collection_id, submission_date,
                   COUNT(*) AS dataset_count,
                   CASE WHEN BOOL_OR(state IN ('PENDING', 'NO_ELECTION')) THEN 'PENDING'
                        WHEN BOOL_AND(state = 'CANCELED') THEN 'CANCELED'
                        WHEN BOOL_AND(state IN ('APPROVED', 'CANCELED')) THEN 'APPROVED'
                        WHEN BOOL_AND(state IN ('DENIED', 'CANCELED')) THEN 'DENIED'
                        ELSE 'MIXED' END AS state,
                   COUNT(*) FILTER (WHERE state IN ('PENDING', 'NO_ELECTION')) AS undecided,
                   COUNT(DISTINCT decided_via) AS via_count,
                   MAX(decided_via) AS any_via,
                   COUNT(*) FILTER (WHERE decided_via IS NOT NULL AND decision_date IS NULL)
                     AS undated,
                   MAX(decision_date) AS last_decision
            FROM pair_decisions
            GROUP BY reference_id, collection_id, submission_date
          ),
          dar_rows AS (
            SELECT reference_id, collection_id, submission_date, dataset_count, state,
                   CASE WHEN undecided > 0 OR via_count = 0 THEN NULL
                        WHEN via_count > 1 THEN 'MIXED'
                        ELSE any_via END AS decided_via,
                   CASE WHEN undecided > 0 OR via_count = 0 OR undated > 0 THEN NULL
                        ELSE last_decision END AS decision_date
            FROM dar_decisions
          )
          """;

  @RegisterConstructorMapper(DecisionBucketCount.class)
  @SqlQuery(
      PAIR_DECISIONS
          + """
          SELECT date_trunc(:bucket, submission_date) AS bucket_start, state, decided_via,
                 COUNT(*) AS count
          FROM pair_decisions
          GROUP BY 1, 2, 3
          ORDER BY 1, 2, 3
          """)
  List<DecisionBucketCount> countPairDecisions(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);

  @RegisterConstructorMapper(DarDatasetDecision.class)
  @SqlQuery(
      PAIR_DECISIONS
          + """
          SELECT reference_id, collection_id, dataset_id, submission_date, state, decided_via,
                 decision_date
          FROM pair_decisions
          ORDER BY submission_date, reference_id, dataset_id
          LIMIT :limit OFFSET :offset
          """)
  List<DarDatasetDecision> findPairDecisions(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("limit") int limit,
      @Bind("offset") int offset);

  @RegisterConstructorMapper(DecisionBucketCount.class)
  @SqlQuery(
      DAR_DECISIONS
          + """
          SELECT date_trunc(:bucket, submission_date) AS bucket_start, state, decided_via,
                 COUNT(*) AS count
          FROM dar_rows
          GROUP BY 1, 2, 3
          ORDER BY 1, 2, 3
          """)
  List<DecisionBucketCount> countDarDecisions(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);

  @RegisterConstructorMapper(DarDecision.class)
  @SqlQuery(
      DAR_DECISIONS
          + """
          SELECT reference_id, collection_id, submission_date, dataset_count, state, decided_via,
                 decision_date
          FROM dar_rows
          ORDER BY submission_date, reference_id
          LIMIT :limit OFFSET :offset
          """)
  List<DarDecision> findDarDecisions(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("limit") int limit,
      @Bind("offset") int offset);
}
