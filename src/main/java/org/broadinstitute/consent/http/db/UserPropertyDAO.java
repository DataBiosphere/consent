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

    String ARE_YOU_PRINCIPAL_INVESTIGATOR = "isThePI";
    String DO_YOU_HAVE_PI = "havePI";
    String PUBMED_ID = "pubmedID";
    String SCIENTIFIC_URL = "scientificURL";


    @SqlQuery("SELECT * FROM user_property WHERE userid = :userId")
    List<UserProperty> findResearcherPropertiesByUser(@Bind("userId") Integer userId);

    @SqlQuery("SELECT propertyvalue FROM USER_PROPERTY WHERE userid = :userId AND propertykey = 'completed'")
    String isProfileCompleted(@Bind("userId") Integer userId);

    @SqlBatch("INSERT INTO user_property (userid, propertykey, propertyvalue) VALUES (:userId, :propertyKey, :propertyValue)")
    void insertAll(@BindBean Collection<UserProperty> researcherProperties);

    @SqlUpdate("DELETE FROM user_property WHERE userid = :userId")
    void deleteAllPropertiesByUser(@Bind("userId") Integer userId);

    @SqlBatch("DELETE FROM user_property WHERE userid = :userId AND propertykey = :propertyKey")
    void deletePropertiesByUserAndKey(@BindBean Collection<UserProperty> researcherProperties);

    @SqlQuery("SELECT * FROM user_property WHERE userid IN (<userIds>)")
    List<UserProperty> findResearcherPropertiesByUserIds(@BindList("userIds") List<Integer> userIds);
}
