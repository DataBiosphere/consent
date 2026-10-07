package org.broadinstitute.consent.http.db;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface ResearcherDashboardDAO {

  /**
   * Every database count the researcher dashboard needs, in one round trip.
   *
   * <p>DAR counts are scoped like {@code
   * DarCollectionSummaryDAO#getDarCollectionSummariesForResearcher}: submitted, non-archived
   * collections the researcher created. Approval counts are scoped like {@code
   * DatasetDAO#getApprovedDatasets}, except that expired approvals are counted rather than filtered
   * out - that filter is why the old Expired stat was always zero.
   */
  @RegisterConstructorMapper(DashboardDatabaseCounts.class)
  @SqlQuery(
      """
      WITH researcher_submissions AS (
        SELECT DISTINCT ON (dar.collection_id)
               dar.collection_id, dar.reference_id, dar.parent_id
        FROM data_access_request dar
        JOIN dar_collection c ON c.collection_id = dar.collection_id
        WHERE dar.submission_date IS NOT NULL
          AND c.create_user_id = :userId
        -- An active original DAR anchors a pre-2022 submission over a canceled sibling.
        ORDER BY dar.collection_id,
                 (dar.parent_id IS NULL AND LOWER(dar.data->>'status') IS NOT DISTINCT FROM 'canceled'),
                 dar.submission_date DESC, dar.id DESC
      ),
      -- Filtering before the DISTINCT ON would substitute an older submission for a collection
      -- whose latest submission is archived, instead of dropping the collection.
      -- A pre-2022 submission is one original DAR per dataset, so read its non-canceled siblings too.
      latest_dar AS (
        SELECT rs.collection_id, dar.reference_id,
               COALESCE(LOWER(dar.data->>'status') = 'canceled', FALSE) AS canceled
        FROM researcher_submissions rs
        JOIN data_access_request dar ON dar.collection_id = rs.collection_id
        WHERE (dar.reference_id = rs.reference_id
               OR (rs.parent_id IS NULL AND dar.parent_id IS NULL
                   AND dar.submission_date IS NOT NULL
                   AND LOWER(COALESCE(dar.data->>'status', '')) != 'canceled'))
          AND (dar.data->>'status' IS NULL OR LOWER(dar.data->>'status') != 'archived')
      ),
      -- Every submitted DAR, not just the latest per collection: an approval granted on an
      -- earlier submission still governs access until it expires.
      submitted_dars AS (
        SELECT dar.reference_id, dar.collection_id, dar.submission_date, c.dar_code
        FROM data_access_request dar
        JOIN dar_collection c ON c.collection_id = dar.collection_id
        WHERE dar.submission_date IS NOT NULL
          AND dar.user_id = :userId
      ),
      final_votes AS (
        SELECT e.reference_id, e.dataset_id, v.vote, v.vote_id, v.create_date, v.update_date
        FROM election e
        JOIN vote v ON v.election_id = e.election_id
        WHERE LOWER(e.election_type) = 'dataaccess'
          AND LOWER(v.type) IN ('final', 'radar_approve')
          AND v.vote IS NOT NULL
          AND (e.reference_id IN (SELECT reference_id FROM latest_dar)
               OR e.reference_id IN (SELECT reference_id FROM submitted_dars))
      ),
      -- Request statuses follow the SO console: for a re-opened election the most recently edited
      -- vote decides.
      latest_final_votes AS (
        SELECT reference_id, dataset_id, vote
        FROM (
          SELECT reference_id, dataset_id, vote,
                 ROW_NUMBER() OVER (
                   PARTITION BY reference_id, dataset_id
                   ORDER BY COALESCE(update_date, create_date) DESC, vote_id DESC
                 ) AS recency
          FROM final_votes
        ) ranked
        WHERE recency = 1
      ),
      -- Approvals instead follow the My Dataset Approvals page, which ranks by create_date, so the
      -- dashboard tile and that page always show the same number. The page breaks create_date ties
      -- arbitrarily; vote_id makes this deterministic.
      page_final_votes AS (
        SELECT reference_id, dataset_id, vote
        FROM (
          SELECT reference_id, dataset_id, vote,
                 ROW_NUMBER() OVER (
                   PARTITION BY reference_id, dataset_id
                   ORDER BY create_date DESC, vote_id DESC
                 ) AS recency
          FROM final_votes
        ) ranked
        WHERE recency = 1
      ),
      -- A canceled DAR neither holds its submission back nor decides it.
      dataset_vote_counts AS (
        SELECT ld.collection_id,
               BOOL_AND(ld.canceled) AS canceled,
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
               COUNT(*) FILTER (WHERE canceled) AS canceled
        FROM dataset_vote_counts
      ),
      -- One row per approved dataset per DAR, as the My Dataset Approvals page lists them.
      -- A closed-out collection no longer grants access, so it drops out.
      approved_datasets AS (
        -- Grouped by dar_code and dataset because that is the key the page's row reducer uses: a
        -- revised DAR has several submissions in one collection, and they are one approval, dated
        -- from the latest submission that granted it.
        SELECT sd.dar_code, dd.dataset_id,
               MAX(sd.submission_date) + make_interval(days => :expirationDays) AS expiration_date
        FROM submitted_dars sd
        JOIN dar_dataset dd ON dd.reference_id = sd.reference_id
        JOIN dataset d ON d.dataset_id = dd.dataset_id
        JOIN dac ON dac.dac_id = d.dac_id AND dac.deleted IS NOT TRUE
        JOIN page_final_votes pfv
          ON pfv.reference_id = sd.reference_id AND pfv.dataset_id = dd.dataset_id
        WHERE pfv.vote
          AND EXISTS (SELECT 1 FROM library_card lc WHERE lc.user_id = :userId)
          AND NOT EXISTS (
            SELECT 1 FROM data_access_request closeout
            WHERE closeout.collection_id = sd.collection_id
              AND closeout.data->>'closeoutSupplement' IS NOT NULL
          )
        GROUP BY sd.dar_code, dd.dataset_id
      )
      SELECT
        COALESCE((SELECT total FROM dar_counts), 0) AS dar_total,
        COALESCE((SELECT approved FROM dar_counts), 0) AS dar_approved,
        COALESCE((SELECT canceled FROM dar_counts), 0) AS dar_canceled,
        -- Active uses the same boundary the approvals page does (submitted within the last year),
        -- so the two never disagree by a row sitting exactly on it.
        (SELECT COUNT(*) FROM approved_datasets WHERE expiration_date > NOW())
          AS approvals_active,
        (SELECT COUNT(*) FROM approved_datasets
          WHERE expiration_date > NOW()
            AND expiration_date <= NOW() + make_interval(days => :expiringSoonDays))
          AS approvals_expiring_soon,
        (SELECT COUNT(*) FROM approved_datasets WHERE expiration_date <= NOW())
          AS approvals_expired
      """)
  DashboardDatabaseCounts getCounts(
      @Bind("userId") Integer userId,
      @Bind("expirationDays") int expirationDays,
      @Bind("expiringSoonDays") int expiringSoonDays);

  record DashboardDatabaseCounts(
      long darTotal,
      long darApproved,
      long darCanceled,
      long approvalsActive,
      long approvalsExpiringSoon,
      long approvalsExpired) {}
}
