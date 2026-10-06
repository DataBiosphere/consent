package org.broadinstitute.consent.http.db;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.consent.http.models.CreatedBucket;
import org.broadinstitute.consent.http.models.RoleUserCount;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * Admin reporting over accounts and institutions created in [:from, :to). There's no login record,
 * so these count accounts created, not active users.
 */
public interface AccountMetricsDAO {

  /** Users created per bucket; a bucket with none is left out. */
  @RegisterConstructorMapper(CreatedBucket.class)
  @SqlQuery(
      """
      SELECT date_trunc(:bucket, create_date) AS bucket_start, COUNT(*) AS count
      FROM users
      WHERE create_date >= :from AND create_date < :to
      GROUP BY 1
      ORDER BY 1
      """)
  List<CreatedBucket> countUsersCreated(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);

  /** Institutions created per bucket; a bucket with none is left out. */
  @RegisterConstructorMapper(CreatedBucket.class)
  @SqlQuery(
      """
      SELECT date_trunc(:bucket, create_date) AS bucket_start, COUNT(*) AS count
      FROM institution
      WHERE create_date >= :from AND create_date < :to
      GROUP BY 1
      ORDER BY 1
      """)
  List<CreatedBucket> countInstitutionsCreated(
      @Bind("from") Instant from, @Bind("to") Instant to, @Bind("bucket") String bucket);

  /**
   * Users created in the range, per role they hold now, most first. A user counts once per role, so
   * the counts can sum past the users created; one with no role isn't counted here.
   */
  @RegisterConstructorMapper(RoleUserCount.class)
  @SqlQuery(
      """
      SELECT r.name AS role, COUNT(DISTINCT u.user_id) AS user_count
      FROM users u
      JOIN user_role ur ON ur.user_id = u.user_id
      JOIN roles r ON r.role_id = ur.role_id
      WHERE u.create_date >= :from AND u.create_date < :to
      GROUP BY r.name
      ORDER BY user_count DESC, r.name
      """)
  List<RoleUserCount> countUsersByRole(@Bind("from") Instant from, @Bind("to") Instant to);
}
