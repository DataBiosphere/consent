package org.broadinstitute.consent.http.db;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface SigningOfficialDashboardDAO {
  @RegisterConstructorMapper(DashboardDatabaseCounts.class)
  @SqlQuery(
      """
      WITH institution_users AS (
        SELECT u.user_id, (lc.id IS NOT NULL) AS active,
               EXISTS (
                 SELECT 1 FROM user_role ur JOIN roles r ON r.role_id = ur.role_id
                 WHERE ur.user_id = u.user_id AND r.name = 'DataSubmitter'
               ) AS data_submitter
        FROM users u
        LEFT JOIN library_card lc ON lc.user_id = u.user_id
        WHERE u.institution_id = :institutionId
      ),
      -- Deliberately NOT institution-scoped: this is the pool of agreements any SO can assign
      -- from, so it is the same count for every institution. Only researchers_approved below is
      -- scoped to the caller's institution.
      assignable_daas AS (
        SELECT DISTINCT daa.daa_id
        FROM data_access_agreement daa
        JOIN dac_daa dd ON dd.daa_id = daa.daa_id
        JOIN dac ON dac.dac_id = dd.dac_id AND dac.deleted IS NOT TRUE
      ),
      latest_submissions AS (
        SELECT DISTINCT ON (dar.collection_id)
               dar.collection_id, dar.reference_id, dar.parent_id
        FROM data_access_request dar
        JOIN dar_collection c ON c.collection_id = dar.collection_id
        JOIN users u ON u.user_id = c.create_user_id
        WHERE dar.submission_date IS NOT NULL
          AND u.institution_id = :institutionId
        ORDER BY dar.collection_id, dar.submission_date DESC, dar.id DESC
      ),
      -- Before 2022-07-27 a submission was saved as one original DAR per dataset, so a latest
      -- original brings in its siblings; their backfilled submission dates need not match.
      -- Archived DARs drop out after the DISTINCT ON so they never surface an older submission.
      latest_dar AS (
        SELECT ls.collection_id, dar.reference_id,
               COALESCE(LOWER(dar.data->>'status') = 'canceled', FALSE) AS canceled,
               -- Needs SO involvement, actioned or not.
               (dar.requires_so_approval
                  OR (dar.data->'closeoutSupplement' IS NOT NULL
                      AND dar.data->'closeoutSupplement' != 'null'::jsonb)) AS needs_so,
               -- The subset of needs_so this SO can still act on.
               ((dar.requires_so_approval AND dar.approving_so_id IS NULL
                   AND LOWER(dar.data->>'signingOfficialEmail') = LOWER(:userEmail))
                  OR (dar.data->'closeoutSupplement' IS NOT NULL
                      AND dar.data->'closeoutSupplement' != 'null'::jsonb
                      AND dar.approving_so_id IS NULL
                      AND dar.data->'closeoutSupplement'->>'signingOfficialId' = :userId))
                 AS awaiting_so
        FROM latest_submissions ls
        JOIN data_access_request dar ON dar.collection_id = ls.collection_id
        WHERE (dar.reference_id = ls.reference_id
               OR (ls.parent_id IS NULL AND dar.parent_id IS NULL
                   AND dar.submission_date IS NOT NULL))
          AND (dar.data->>'status' IS NULL OR LOWER(dar.data->>'status') != 'archived')
      ),
      ranked_final_votes AS (
        SELECT e.reference_id, e.dataset_id, v.vote,
               ROW_NUMBER() OVER (
                 PARTITION BY e.reference_id, e.dataset_id
                 ORDER BY COALESCE(v.update_date, v.create_date) DESC, v.vote_id DESC
               ) AS recency
        FROM election e
        JOIN latest_dar ld ON ld.reference_id = e.reference_id
        JOIN vote v ON v.election_id = e.election_id
        WHERE LOWER(e.election_type) = 'dataaccess'
          AND LOWER(v.type) IN ('final', 'radar_approve')
          AND v.vote IS NOT NULL
      ),
      latest_final_votes AS (
        SELECT reference_id, dataset_id, vote
        FROM ranked_final_votes
        WHERE recency = 1
      ),
      -- A canceled sibling neither holds a submission back nor decides it.
      institution_dars AS (
        SELECT ld.collection_id,
               BOOL_AND(ld.canceled) AS canceled,
               BOOL_OR(ld.needs_so) AS needs_so,
               BOOL_OR(ld.awaiting_so) AS awaiting_so,
               COUNT(DISTINCT dd.dataset_id) FILTER (WHERE NOT ld.canceled) AS dataset_count,
               COUNT(DISTINCT lfv.dataset_id) FILTER (WHERE lfv.vote AND NOT ld.canceled)
                 AS approved_dataset_count
        FROM latest_dar ld
        JOIN dar_dataset dd ON dd.reference_id = ld.reference_id
        LEFT JOIN latest_final_votes lfv
          ON lfv.reference_id = ld.reference_id AND lfv.dataset_id = dd.dataset_id
        GROUP BY ld.collection_id
      ),
      dar_counts AS (
        SELECT COUNT(*) AS total,
               COUNT(*) FILTER (
                 WHERE dataset_count > 0 AND approved_dataset_count >= dataset_count
               ) AS approved,
               COUNT(*) FILTER (WHERE canceled) AS canceled,
               COUNT(*) FILTER (WHERE needs_so) AS approval_total,
               COUNT(*) FILTER (WHERE awaiting_so) AS awaiting_so_action
        FROM institution_dars
      )
      SELECT
        (SELECT COUNT(*) FROM institution_users WHERE active) AS active_researchers,
        (SELECT COUNT(*) FROM institution_users WHERE NOT active) AS inactive_researchers,
        COALESCE((SELECT total FROM dar_counts), 0) AS dar_total,
        COALESCE((SELECT approved FROM dar_counts), 0) AS dar_approved,
        COALESCE((SELECT canceled FROM dar_counts), 0) AS dar_canceled,
        COALESCE((SELECT approval_total FROM dar_counts), 0) AS approval_total,
        COALESCE((SELECT awaiting_so_action FROM dar_counts), 0) AS awaiting_so_action,
        (SELECT COUNT(*) FROM institution_users WHERE data_submitter) AS approved_data_submitters,
        (SELECT COUNT(*) FROM assignable_daas) AS agreements,
        (SELECT COUNT(DISTINCT iu.user_id)
           FROM institution_users iu
           JOIN library_card lc ON lc.user_id = iu.user_id
           JOIN lc_daa ld ON ld.lc_id = lc.id
           JOIN assignable_daas ad ON ad.daa_id = ld.daa_id) AS researchers_approved
      """)
  DashboardDatabaseCounts getCounts(
      @Bind("institutionId") Integer institutionId,
      @Bind("userId") String userId,
      @Bind("userEmail") String userEmail);

  record DashboardDatabaseCounts(
      long activeResearchers,
      long inactiveResearchers,
      long darTotal,
      long darApproved,
      long darCanceled,
      long approvalTotal,
      long awaitingSoAction,
      long approvedDataSubmitters,
      long agreements,
      long researchersApproved) {}
}
