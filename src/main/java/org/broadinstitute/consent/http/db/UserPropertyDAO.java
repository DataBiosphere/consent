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

    String INSTITUTION = "institution";
    String ARE_YOU_PRINCIPAL_INVESTIGATOR = "isThePI";
    String DO_YOU_HAVE_PI = "havePI";
    String ERA_COMMONS_ID = "eRACommonsID";
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

    @SqlUpdate("DELETE FROM user_property WHERE userid = :userId AND propertykey IN (<propertyKeyList>)")
    void deletePropertyByUser(@BindList("propertyKeyList") List<String> propertyKeyList, @Bind("userId") Integer userId);

    @SqlQuery(value = "SELECT * FROM user_property WHERE " +
            "(propertykey = '" + INSTITUTION + "' AND propertyvalue != :institutionName) OR " +
            "(propertykey = '" + ARE_YOU_PRINCIPAL_INVESTIGATOR + "' AND  propertyvalue != :isThePI) OR " +
            "(propertykey = '" + DO_YOU_HAVE_PI + "' AND  propertyvalue != :havePI) OR " +
            "(propertykey = '" + ERA_COMMONS_ID + "' AND  propertyvalue != :eRACommonsID) OR " +
            "(propertykey = '" + PUBMED_ID + "' AND  propertyvalue != :pubmedID) OR " +
            "(propertykey = '" + SCIENTIFIC_URL + "' AND  propertyvalue != :scientificURL) " +
            " AND userid = :userId")
    List<UserProperty> findResearcherProperties(@Bind("userId") Integer userId, @Bind("institutionName") String institutionName,@Bind("isThePI") String isThePI,
                                                      @Bind("havePI") String havePI, @Bind("eRACommonsID") String eRACommonsID, @Bind("pubmedID") String pubmedID, @Bind("scientificURL") String scientificURL);

    @SqlQuery("SELECT propertyvalue FROM user_property WHERE userid = :userId and propertykey = :propertyKey")
    String findPropertyValueByPK(@Bind("userId") Integer userId, @Bind("propertyKey") String propertyKey);

    @SqlQuery("SELECT * FROM user_property WHERE userid IN (<userIds>)")
    List<UserProperty> findResearcherPropertiesByUserIds(@BindList("userIds") List<Integer> userIds);
}
