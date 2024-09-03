package org.broadinstitute.consent.http.db;

import java.util.Collection;
import java.util.List;
import org.broadinstitute.consent.http.db.mapper.UserPropertyMapper;
import org.broadinstitute.consent.http.models.UserProperty;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(UserPropertyMapper.class)
public interface UserPropertyDAO extends Transactional<UserPropertyDAO> {

  @SqlQuery("""
    SELECT * FROM user_property WHERE user_id = :userId AND property_key IN (<keys>)
    """)
  List<UserProperty> findUserPropertiesByUserIdAndPropertyKeys(@Bind("userId") Integer userId,
      @BindList("keys") List<String> keys);

  @SqlBatch("""
          INSERT INTO user_property (user_id, property_key, property_value)
          VALUES (:userId, :propertyKey, :propertyValue)
          ON CONFLICT (user_id, property_key)
          DO UPDATE SET property_value = :propertyValue
      """)
  void insertAll(@BindBean Collection<UserProperty> researcherProperties);

  @SqlUpdate("""
    DELETE FROM user_property WHERE user_id = :userId
    """)
  void deleteAllPropertiesByUser(@Bind("userId") Integer userId);

  @SqlBatch("""
    DELETE FROM user_property WHERE user_id = :userId AND property_key = :propertyKey
    """)
  void deletePropertiesByUserAndKey(@BindBean Collection<UserProperty> researcherProperties);
}
