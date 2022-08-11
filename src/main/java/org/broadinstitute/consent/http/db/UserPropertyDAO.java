package org.broadinstitute.consent.http.db;

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

import java.util.Collection;
import java.util.List;

@RegisterRowMapper(UserPropertyMapper.class)
public interface UserPropertyDAO extends Transactional<UserPropertyDAO> {

    @SqlQuery("SELECT * FROM user_property WHERE userid = :userId AND propertykey IN (<properties>)")
    List<UserProperty> findResearcherPropertiesByUser(@Bind("userId") Integer userId,
                                                      @BindList("properties") List<String> properties);

    @SqlBatch("INSERT INTO user_property (userid, propertykey, propertyvalue) VALUES (:userId, :propertyKey, :propertyValue)")
    void insertAll(@BindBean Collection<UserProperty> researcherProperties);

    @SqlUpdate("DELETE FROM user_property WHERE userid = :userId")
    void deleteAllPropertiesByUser(@Bind("userId") Integer userId);

    @SqlBatch("DELETE FROM user_property WHERE userid = :userId AND propertykey = :propertyKey")
    void deletePropertiesByUserAndKey(@BindBean Collection<UserProperty> researcherProperties);
}
