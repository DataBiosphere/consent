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

  @SqlQuery("SELECT * FROM user_property WHERE userid = :userId AND propertykey IN (<keys>)")
  List<UserProperty> findUserPropertiesByUserIdAndPropertyKeys(@Bind("userId") Integer userId,
      @BindList("keys") List<String> keys);

  @SqlBatch("""
          INSERT INTO user_property (userid, propertykey, propertyvalue)
          VALUES (:userId, :propertyKey, :propertyValue)
          ON CONFLICT (userid, propertykey)
          DO UPDATE SET propertyvalue = :propertyValue
      """)
  void insertAll(@BindBean Collection<UserProperty> userProperties);

  @SqlUpdate("DELETE FROM user_property WHERE userid = :userId")
  void deleteAllPropertiesByUser(@Bind("userId") Integer userId);

  @SqlBatch("DELETE FROM user_property WHERE userid = :userId AND propertykey = :propertyKey")
  void deletePropertiesByUserAndKey(@BindBean Collection<UserProperty> userProperties);
}
