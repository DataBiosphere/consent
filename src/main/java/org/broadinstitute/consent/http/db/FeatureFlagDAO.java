package org.broadinstitute.consent.http.db;

import java.util.List;
import org.broadinstitute.consent.http.db.mapper.FeatureFlagMapper;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(FeatureFlagMapper.class)
public interface FeatureFlagDAO extends Transactional<FeatureFlagDAO> {

  /**
   * Find all feature flags
   *
   * @return List<FeatureFlag>
   */
  @SqlQuery("SELECT id, value, create_date, update_date FROM feature_flag ORDER BY id")
  List<FeatureFlag> findAll();

  /**
   * Find a feature flag by id
   *
   * @param id The feature flag id
   * @return FeatureFlag or null if not found
   */
  @SqlQuery("SELECT id, value, create_date, update_date FROM feature_flag WHERE id = :id")
  FeatureFlag findById(@Bind("id") String id);

  /**
   * Insert a new feature flag
   *
   * @param id The feature flag id
   * @param value The feature flag value
   */
  @SqlUpdate(
      """
      INSERT INTO feature_flag (id, value, create_date, update_date)
      VALUES (:id, :value, NOW(), NOW())
      """)
  void insert(@Bind("id") String id, @Bind("value") String value);

  /**
   * Update an existing feature flag
   *
   * @param id The feature flag id
   * @param value The new feature flag value
   */
  @SqlUpdate("UPDATE feature_flag SET value = :value, update_date = NOW() WHERE id = :id")
  void update(@Bind("id") String id, @Bind("value") String value);

  /**
   * Delete a feature flag by id
   *
   * @param id The feature flag id
   */
  @SqlUpdate("DELETE FROM feature_flag WHERE id = :id")
  void deleteById(@Bind("id") String id);

  /**
   * Check if a feature flag exists
   *
   * @param id The feature flag id
   * @return true if exists, false otherwise
   */
  @SqlQuery("SELECT COUNT(*) > 0 FROM feature_flag WHERE id = :id")
  boolean exists(@Bind("id") String id);
}
