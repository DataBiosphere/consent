package org.broadinstitute.consent.http.db;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface AdminDashboardDAO {
  /** DAR counts follow {@link SigningOfficialDashboardDAO} across every institution. */
  @RegisterConstructorMapper(DashboardDatabaseCounts.class)
  @SqlQuery(
      """
      WITH assignable_daas AS (
        SELECT DISTINCT daa.daa_id
        FROM data_access_agreement daa
        JOIN dac_daa dd ON dd.daa_id = daa.daa_id
        JOIN dac ON dac.dac_id = dd.dac_id AND dac.deleted IS NOT TRUE
      ),
      submissions AS (
        SELECT DISTINCT ON (dar.collection_id)
               dar.collection_id, dar.reference_id, dar.data
        FROM data_access_request dar
        WHERE dar.submission_date IS NOT NULL
        ORDER BY dar.collection_id, dar.submission_date DESC, dar.id DESC
      ),
      -- Archived collections drop out entirely. Filtering before the DISTINCT ON would instead
      -- substitute an older submission for a collection whose latest submission is archived.
      latest_dar AS (
        SELECT collection_id, reference_id, data
        FROM submissions
        WHERE data->>'status' IS NULL OR LOWER(data->>'status') != 'archived'
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
      dataset_vote_counts AS (
        SELECT ld.collection_id,
               COUNT(DISTINCT dd.dataset_id) AS dataset_count,
               COUNT(DISTINCT lfv.dataset_id) FILTER (WHERE lfv.vote) AS approved_dataset_count
        FROM latest_dar ld
        JOIN dar_dataset dd ON dd.reference_id = ld.reference_id
        LEFT JOIN latest_final_votes lfv
          ON lfv.reference_id = ld.reference_id AND lfv.dataset_id = dd.dataset_id
        GROUP BY ld.collection_id
      ),
      dar_counts AS (
        SELECT COUNT(*) AS total,
               COUNT(*) FILTER (
                 WHERE dvc.dataset_count > 0
                   AND dvc.approved_dataset_count >= dvc.dataset_count
                   AND LOWER(ld.data->>'status') IS DISTINCT FROM 'canceled'
               ) AS approved,
               COUNT(*) FILTER (WHERE LOWER(ld.data->>'status') = 'canceled') AS canceled
        FROM latest_dar ld
        JOIN dataset_vote_counts dvc ON dvc.collection_id = ld.collection_id
      )
      SELECT
        COALESCE((SELECT total FROM dar_counts), 0) AS dar_total,
        COALESCE((SELECT approved FROM dar_counts), 0) AS dar_approved,
        COALESCE((SELECT canceled FROM dar_counts), 0) AS dar_canceled,
        (SELECT COUNT(*) FROM dac WHERE deleted IS NOT TRUE) AS dacs,
        (SELECT COUNT(*) FROM users) AS users,
        (SELECT COUNT(*) FROM institution) AS institutions,
        (SELECT COUNT(*) FROM institution i
           WHERE NOT EXISTS (
             SELECT 1 FROM users u
             JOIN user_role ur ON ur.user_id = u.user_id
             JOIN roles r ON r.role_id = ur.role_id
             WHERE u.institution_id = i.institution_id AND r.name = 'SigningOfficial'
           )) AS institutions_without_signing_official,
        (SELECT COUNT(*) FROM library_card) AS library_cards,
        (SELECT COUNT(*) FROM assignable_daas) AS agreements,
        (SELECT COUNT(DISTINCT lc.user_id)
           FROM library_card lc
           JOIN lc_daa ld ON ld.lc_id = lc.id
           JOIN assignable_daas ad ON ad.daa_id = ld.daa_id) AS researchers_approved
      """)
  DashboardDatabaseCounts getCounts();

  record DashboardDatabaseCounts(
      long darTotal,
      long darApproved,
      long darCanceled,
      long dacs,
      long users,
      long institutions,
      long institutionsWithoutSigningOfficial,
      long libraryCards,
      long agreements,
      long researchersApproved) {}
}
