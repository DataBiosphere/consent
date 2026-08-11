package org.broadinstitute.consent.http.db;

import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface DacDashboardDAO {

  /**
   * Returns every relational count needed by the DAC dashboard in one database round trip.
   *
   * <p>DAR scope and status deliberately mirror {@link
   * DarCollectionSummaryDAO#getDarCollectionSummariesForDACRole}: the caller's selected DAC role,
   * the latest submitted non-archived DAR, and the latest active or terminal data-access election
   * for each dataset. A collection is complete only when every relevant dataset has an election and
   * none is open.
   */
  @RegisterConstructorMapper(DashboardDatabaseCounts.class)
  @SqlQuery(
      """
      WITH user_dacs AS (
        SELECT DISTINCT ur.dac_id
        FROM user_role ur
        JOIN dac ON dac.dac_id = ur.dac_id AND dac.deleted IS NOT TRUE
        WHERE ur.user_id = :userId
          AND ur.role_id = :roleId
          AND ur.dac_id IS NOT NULL
      ),
      latest_dar AS (
        SELECT DISTINCT ON (dar.collection_id)
               dar.collection_id, dar.reference_id, dar.data->'closeoutSupplement' AS closeout
        FROM data_access_request dar
        WHERE dar.submission_date IS NOT NULL
          AND (LOWER(dar.data->>'status') != 'archived' OR dar.data->>'status' IS NULL)
        ORDER BY dar.collection_id, dar.submission_date DESC
      ),
      relevant_datasets AS (
        SELECT DISTINCT ld.collection_id, ld.reference_id, ld.closeout, dd.dataset_id
        FROM latest_dar ld
        JOIN dar_dataset dd ON dd.reference_id = ld.reference_id
        JOIN dataset d ON d.dataset_id = dd.dataset_id
        JOIN user_dacs ud ON ud.dac_id = d.dac_id
      ),
      ranked_elections AS (
        SELECT e.election_id, e.reference_id, e.dataset_id, e.status,
               ROW_NUMBER() OVER (
                 PARTITION BY e.reference_id, e.dataset_id ORDER BY e.election_id DESC
               ) AS recency
        FROM election e
        JOIN relevant_datasets rd
          ON rd.reference_id = e.reference_id AND rd.dataset_id = e.dataset_id
        WHERE LOWER(e.election_type) = 'dataaccess'
          AND LOWER(e.status) IN ('open', 'closed', 'canceled')
      ),
      latest_elections AS (
        SELECT re.election_id, re.reference_id, re.dataset_id, re.status,
               EXISTS (
                 SELECT 1
                 FROM vote v
                 WHERE v.election_id = re.election_id
                   AND v.user_id = :userId
                   AND LOWER(v.type) = 'dac'
                   AND v.vote IS NULL
               ) AS has_pending_member_vote
        FROM ranked_elections re
        WHERE recency = 1
      ),
      collection_state AS (
        SELECT rd.collection_id,
               COUNT(DISTINCT rd.dataset_id) AS dataset_count,
               COUNT(DISTINCT le.dataset_id) AS election_count,
               COALESCE(BOOL_OR(LOWER(le.status) = 'open'), FALSE) AS has_open_election,
               COALESCE(BOOL_OR(rd.closeout IS NOT NULL AND rd.closeout != 'null'::jsonb), FALSE)
                 AS has_closeout,
               COALESCE(BOOL_OR(le.has_pending_member_vote), FALSE)
                 AS has_pending_member_vote
        FROM relevant_datasets rd
        LEFT JOIN latest_elections le
          ON le.reference_id = rd.reference_id AND le.dataset_id = rd.dataset_id
        GROUP BY rd.collection_id
      )
      SELECT
        (SELECT COUNT(*) FROM user_dacs) AS dacs,
        COUNT(*) AS dar_total,
        COUNT(*) FILTER (
          WHERE election_count >= dataset_count AND NOT has_open_election
        ) AS dar_approved,
        COUNT(*) FILTER (
          WHERE has_open_election
            AND ((:isChair AND NOT has_closeout)
              OR (NOT :isChair AND has_pending_member_vote))
        ) AS awaiting_my_vote
      FROM collection_state
      """)
  DashboardDatabaseCounts getCounts(
      @Bind("userId") Integer userId,
      @Bind("roleId") Integer roleId,
      @Bind("isChair") boolean isChair);

  record DashboardDatabaseCounts(long dacs, long darTotal, long darApproved, long awaitingMyVote) {}
}
