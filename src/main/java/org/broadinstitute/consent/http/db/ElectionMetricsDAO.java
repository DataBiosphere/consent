package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.ElectionBucket;
import org.broadinstitute.consent.http.models.VoteBucket;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * Reporting on DAC activity volume over data access elections in [:from, :to). Decision outcomes
 * are DarMetricsDAO's; these count elections opened and votes cast. :dacIds scopes a query to
 * elections on datasets now in those DACs, null to every dataset.
 */
public interface ElectionMetricsDAO {

  /**
   * Elections opened per bucket and status they have now, in ElectionStatus's spelling; a bucket
   * with none is left out.
   */
  @RegisterConstructorMapper(ElectionBucket.class)
  @SqlQuery(
      """
      SELECT date_trunc(:bucket, create_date) AS bucket_start,
             CASE LOWER(status)
               WHEN 'open' THEN 'Open'
               WHEN 'closed' THEN 'Closed'
               WHEN 'canceled' THEN 'Canceled'
               WHEN 'final' THEN 'Final'
               WHEN 'pendingapproval' THEN 'PendingApproval'
               ELSE status
             END AS status,
             COUNT(*) AS count
      FROM election
      WHERE LOWER(election_type) = 'dataaccess'
        AND create_date >= :from AND create_date < :to
        AND (CAST(:dacIds AS int[]) IS NULL
             OR dataset_id IN (SELECT dataset_id FROM dataset
                               WHERE dac_id = ANY(CAST(:dacIds AS int[]))))
      GROUP BY 1, 2
      ORDER BY 1, 2
      """)
  List<ElectionBucket> countElectionsOpened(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("dacIds") List<Integer> dacIds,
      @Bind("bucket") String bucket);

  /**
   * Votes cast per bucket and VoteType, dated by when they were last cast, or created when that
   * wasn't recorded. Opening an election inserts its votes uncast, so those aren't counted.
   */
  @RegisterConstructorMapper(VoteBucket.class)
  @SqlQuery(
      """
      SELECT date_trunc(:bucket, COALESCE(v.update_date, v.create_date)) AS bucket_start,
             CASE WHEN LOWER(v.type) = 'chairperson' THEN 'Chairperson' ELSE UPPER(v.type) END
               AS type,
             COUNT(*) AS count
      FROM vote v
      JOIN election e ON e.election_id = v.election_id
      WHERE LOWER(e.election_type) = 'dataaccess'
        AND v.vote IS NOT NULL
        AND COALESCE(v.update_date, v.create_date) >= :from
        AND COALESCE(v.update_date, v.create_date) < :to
        AND (CAST(:dacIds AS int[]) IS NULL
             OR e.dataset_id IN (SELECT dataset_id FROM dataset
                                 WHERE dac_id = ANY(CAST(:dacIds AS int[]))))
      GROUP BY 1, 2
      ORDER BY 1, 2
      """)
  List<VoteBucket> countVotesCast(
      @Bind("from") Instant from,
      @Bind("to") Instant to,
      @Bind("dacIds") List<Integer> dacIds,
      @Bind("bucket") String bucket);
}
