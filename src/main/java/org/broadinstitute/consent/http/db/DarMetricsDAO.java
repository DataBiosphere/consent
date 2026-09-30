package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.DarDatasetDecision;
import org.broadinstitute.consent.http.models.DarDatasetTurnaround;
import org.broadinstitute.consent.http.models.DarDecision;
import org.broadinstitute.consent.http.models.DarTurnaround;
import org.broadinstitute.consent.http.models.DarVolume;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.InstitutionDarCount;
import org.broadinstitute.consent.http.models.ResearcherDarCount;
import org.broadinstitute.consent.http.models.SoApproval;
import org.broadinstitute.consent.http.models.SoApprovalBucket;
import org.broadinstitute.consent.http.models.TurnaroundBucket;
import org.broadinstitute.consent.http.models.VolumeBucketCount;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * Admin reporting over DAR decisions and submission volume. Every query is bounded by a submission
 * date range.
 */
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
   * Rolls pairs up per submission, which before 2022-07-27 was saved as one DAR per dataset, so a
   * collection's original DARs roll up together under its earliest. A submission is decided once no
   * pair is pending; canceled pairs don't hold it open or affect its outcome, and one whose pairs
   * were all canceled is canceled. Its decision date is the last pair decision, left null when any
   * deciding vote predates decision dates.
   */
  String DAR_DECISIONS =
      PAIR_DECISIONS
          + """
          , dar_decisions AS (
            SELECT (ARRAY_AGG(reference_id ORDER BY submission_date, reference_id))[1]
                     AS reference_id,
                   collection_id, MIN(submission_date) AS submission_date,
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
            GROUP BY COALESCE(collection_id::text, reference_id), collection_id
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

  /**
   * Decided pairs, from submission to the deciding vote. A vote with no update date, or one dated
   * before a backfilled submission date, is kept with a null elapsed time, so it is counted but not
   * measured; the vote's create date is when the election opened, which would understate
   * turnaround.
   */
  String PAIR_TURNAROUND =
      PAIR_DECISIONS
          + """
          , turnaround AS (
            SELECT reference_id, collection_id, dataset_id, submission_date, decision_date,
                   decided_via,
                   CASE WHEN decision_date >= submission_date
                        THEN EXTRACT(EPOCH FROM decision_date - submission_date)::float8 / 86400
                        END AS elapsed_days
            FROM pair_decisions
            WHERE decided_via IS NOT NULL
          )
          """;

  /**
   * Decided submissions, from submission to their last pair decision. One with any undated deciding
   * vote, or decided before its submission date, is counted but not measured.
   */
  String DAR_TURNAROUND =
      DAR_DECISIONS
          + """
          , turnaround AS (
            SELECT reference_id, collection_id, submission_date, decision_date, decided_via,
                   CASE WHEN decision_date >= submission_date
                        THEN EXTRACT(EPOCH FROM decision_date - submission_date)::float8 / 86400
                        END AS elapsed_days
            FROM dar_rows
            WHERE state IN ('APPROVED', 'DENIED', 'MIXED')
          )
          """;

  String TURNAROUND_BUCKETS =
      """
      SELECT date_trunc(:bucket, submission_date) AS bucket_start,
             COUNT(elapsed_days) AS count,
             COUNT(*) - COUNT(elapsed_days) AS unmeasured,
             AVG(elapsed_days) AS mean_days,
             PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY elapsed_days) AS median_days,
             (MODE() WITHIN GROUP (ORDER BY FLOOR(elapsed_days)))::int AS mode_days
      FROM turnaround
      GROUP BY 1
      ORDER BY 1
      """;

  @RegisterConstructorMapper(TurnaroundBucket.class)
  @SqlQuery(PAIR_TURNAROUND + TURNAROUND_BUCKETS)
  List<TurnaroundBucket> countPairTurnaround(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);

  @RegisterConstructorMapper(DarDatasetTurnaround.class)
  @SqlQuery(
      PAIR_TURNAROUND
          + """
          SELECT reference_id, collection_id, dataset_id, submission_date, decision_date,
                 decided_via, elapsed_days
          FROM turnaround
          WHERE elapsed_days IS NOT NULL
          ORDER BY submission_date, reference_id, dataset_id
          LIMIT :limit OFFSET :offset
          """)
  List<DarDatasetTurnaround> findPairTurnaround(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("limit") int limit,
      @Bind("offset") int offset);

  @RegisterConstructorMapper(TurnaroundBucket.class)
  @SqlQuery(DAR_TURNAROUND + TURNAROUND_BUCKETS)
  List<TurnaroundBucket> countDarTurnaround(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);

  @RegisterConstructorMapper(DarTurnaround.class)
  @SqlQuery(
      DAR_TURNAROUND
          + """
          SELECT reference_id, collection_id, submission_date, decision_date, decided_via,
                 elapsed_days
          FROM turnaround
          WHERE elapsed_days IS NOT NULL
          ORDER BY submission_date, reference_id
          LIMIT :limit OFFSET :offset
          """)
  List<DarTurnaround> findDarTurnaround(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("limit") int limit,
      @Bind("offset") int offset);

  /**
   * One row per DAR, progress report or closeout submitted in [:from, :to), with where it stands
   * with its signing official. {@code requires_so_approval} is only ever written true, so NULL is a
   * pre-authorization skip, but only from 20 May 2026, when production first wrote it; closeouts
   * always go to an SO and stay NULL. Closeout approvals were recorded from 5 June 2025. Before
   * 2022-07-27 a submission was saved as one DAR per dataset, so a collection's original DARs count
   * as one submission, reported under its earliest.
   */
  String SO_APPROVALS =
      """
      WITH so_rows AS (
        SELECT dar.reference_id, dar.collection_id, dar.submission_date, dar.requires_so_approval,
               dar.approving_so_timestamp AS approval_date,
               CASE WHEN dar.parent_id IS NULL
                    THEN COALESCE(dar.collection_id::text, dar.reference_id)
                    ELSE dar.reference_id END AS submission_key,
               CASE WHEN dar.parent_id IS NULL THEN 'ORIGINAL'
                    WHEN dar.data->>'closeoutSupplement' IS NOT NULL THEN 'CLOSEOUT'
                    ELSE 'PROGRESS_REPORT' END AS kind
        FROM data_access_request dar
        WHERE dar.submission_date >= :from AND dar.submission_date < :to
          AND (dar.data->>'status' IS NULL
               OR LOWER(dar.data->>'status') NOT IN ('canceled', 'archived'))
      ),
      submissions AS (
        SELECT (ARRAY_AGG(reference_id ORDER BY submission_date, reference_id))[1] AS reference_id,
               MIN(collection_id) AS collection_id, kind, MIN(submission_date) AS submission_date,
               BOOL_OR(requires_so_approval) AS requires_so_approval,
               MIN(approval_date) AS approval_date
        FROM so_rows
        GROUP BY submission_key, kind
      ),
      so_approvals AS (
        SELECT reference_id, collection_id, kind, submission_date, approval_date,
               CASE WHEN approval_date IS NOT NULL THEN 'APPROVED'
                    WHEN kind = 'CLOSEOUT' AND submission_date >= '2025-06-05' THEN 'PENDING'
                    WHEN kind = 'CLOSEOUT' THEN 'NOT_DETERMINED'
                    WHEN requires_so_approval THEN 'PENDING'
                    WHEN submission_date >= '2026-05-20' THEN 'SKIPPED'
                    ELSE 'NOT_DETERMINED' END AS status,
               CASE WHEN approval_date >= submission_date
                    THEN EXTRACT(EPOCH FROM approval_date - submission_date)::float8 / 86400
                    END AS elapsed_days
        FROM submissions
      )
      """;

  @RegisterConstructorMapper(SoApprovalBucket.class)
  @SqlQuery(
      SO_APPROVALS
          + """
          SELECT date_trunc(:bucket, submission_date) AS bucket_start, kind, status,
                 COUNT(*) AS count,
                 COUNT(*) FILTER (WHERE status = 'APPROVED' AND elapsed_days IS NULL)
                   AS unmeasured,
                 AVG(elapsed_days) AS mean_days,
                 PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY elapsed_days) AS median_days,
                 (MODE() WITHIN GROUP (ORDER BY FLOOR(elapsed_days)))::int AS mode_days
          FROM so_approvals
          GROUP BY 1, 2, 3
          ORDER BY 1, 2, 3
          """)
  List<SoApprovalBucket> countSoApprovals(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);

  @RegisterConstructorMapper(SoApproval.class)
  @SqlQuery(
      SO_APPROVALS
          + """
          SELECT reference_id, collection_id, kind, submission_date, status, approval_date,
                 elapsed_days
          FROM so_approvals
          ORDER BY submission_date, reference_id
          LIMIT :limit OFFSET :offset
          """)
  List<SoApproval> findSoApprovals(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("limit") int limit,
      @Bind("offset") int offset);

  /**
   * One row per original DAR submitted in [:from, :to). The institution is the one recorded at
   * submission, falling back to the submitter's current one only for DARs submitted before
   * submissions recorded it; a recorded null stays null. Its name is read through the id, so an
   * admin rename shows, and the recorded name is used only once the institution has been deleted.
   * External collaborators aren't counted: they are approved separately from the DAR. Before
   * 2022-07-27 a submission was saved as one DAR per dataset, so each collection's original DARs
   * count as one submission, reported under its earliest.
   */
  String DAR_VOLUME =
      """
      WITH original_dars AS (
        SELECT dar.reference_id, dar.collection_id, dar.user_id, dar.submission_date,
               COALESCE(dar.collection_id::text, dar.reference_id) AS submission_key,
               CASE WHEN dar.institution_snapshot_date IS NOT NULL THEN dar.institution_id
                    ELSE u.institution_id END AS institution_id,
               CASE WHEN dar.institution_snapshot_date IS NOT NULL THEN 'RECORDED'
                    ELSE 'CURRENT' END AS institution_source,
               dar.institution_name AS recorded_name,
               CASE WHEN jsonb_typeof(dar.data->'labCollaborators') = 'array'
                    THEN jsonb_array_length(dar.data->'labCollaborators') ELSE 0
                    END AS lab_staff_count,
               CASE WHEN jsonb_typeof(dar.data->'internalCollaborators') = 'array'
                    THEN jsonb_array_length(dar.data->'internalCollaborators') ELSE 0
                    END AS internal_collaborator_count
        FROM data_access_request dar
        LEFT JOIN users u ON u.user_id = dar.user_id
        WHERE dar.parent_id IS NULL
          AND dar.submission_date >= :from AND dar.submission_date < :to
          AND (dar.data->>'status' IS NULL
               OR LOWER(dar.data->>'status') NOT IN ('canceled', 'archived'))
      ),
      dar_volume AS (
        SELECT DISTINCT ON (od.submission_key)
               od.reference_id, od.collection_id, od.user_id, od.submission_date,
               od.institution_id, COALESCE(i.institution_name, od.recorded_name) AS institution_name,
               od.institution_source,
               (SUM(ds.dataset_count) OVER (PARTITION BY od.submission_key))::int AS dataset_count,
               od.lab_staff_count, od.internal_collaborator_count
        FROM original_dars od
        LEFT JOIN institution i ON i.institution_id = od.institution_id
        CROSS JOIN LATERAL (
          SELECT COUNT(*) AS dataset_count FROM dar_dataset dd
          WHERE dd.reference_id = od.reference_id
        ) ds
        ORDER BY od.submission_key, od.submission_date, od.reference_id
      )
      """;

  /**
   * Per-bucket totals; a DAR with no institution isn't counted among the institutions, and a
   * deleted one still counts, by its recorded name.
   */
  @RegisterConstructorMapper(VolumeBucketCount.class)
  @SqlQuery(
      DAR_VOLUME
          + """
          SELECT date_trunc(:bucket, submission_date) AS bucket_start,
                 COUNT(*) AS dar_count,
                 COUNT(DISTINCT user_id) AS researcher_count,
                 COUNT(DISTINCT (institution_id, institution_name))
                   FILTER (WHERE institution_name IS NOT NULL) AS institution_count,
                 SUM(dataset_count) AS dataset_count
          FROM dar_volume
          GROUP BY 1
          ORDER BY 1
          """)
  List<VolumeBucketCount> countDarVolume(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);

  /**
   * DARs and distinct researchers per institution across the range, most DARs first. DARs with no
   * institution form one group with a null id and name; a deleted institution keeps its own group,
   * under its recorded name with a null id.
   */
  @RegisterConstructorMapper(InstitutionDarCount.class)
  @SqlQuery(
      DAR_VOLUME
          + """
          SELECT institution_id, institution_name,
                 COUNT(*) AS dar_count, COUNT(DISTINCT user_id) AS researcher_count
          FROM dar_volume
          GROUP BY institution_id, institution_name
          ORDER BY dar_count DESC, institution_name NULLS LAST, institution_id
          """)
  List<InstitutionDarCount> countDarsByInstitution(
      @Bind("from") Instant from, @Bind("to") Instant to);

  /** DARs per submitter across the range, most DARs first. */
  @RegisterConstructorMapper(ResearcherDarCount.class)
  @SqlQuery(
      DAR_VOLUME
          + """
          SELECT user_id, COUNT(*) AS dar_count
          FROM dar_volume
          GROUP BY user_id
          ORDER BY dar_count DESC, user_id
          """)
  List<ResearcherDarCount> countDarsByResearcher(
      @Bind("from") Instant from, @Bind("to") Instant to);

  @RegisterConstructorMapper(DarVolume.class)
  @SqlQuery(
      DAR_VOLUME
          + """
          SELECT reference_id, collection_id, user_id, submission_date, institution_id,
                 institution_name, institution_source, dataset_count, lab_staff_count,
                 internal_collaborator_count
          FROM dar_volume
          ORDER BY submission_date, reference_id
          LIMIT :limit OFFSET :offset
          """)
  List<DarVolume> findDarVolume(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("limit") int limit,
      @Bind("offset") int offset);
}
