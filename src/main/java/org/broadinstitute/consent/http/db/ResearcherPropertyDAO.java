package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.ResearcherProperty;
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

@RegisterRowMapper(ResearcherPropertyMapper.class)
public interface ResearcherPropertyDAO extends Transactional<ResearcherPropertyDAO> {

    String INSTITUTION = "institution";
    String ARE_YOU_PRINCIPAL_INVESTIGATOR = "isThePI";
    String DO_YOU_HAVE_PI = "havePI";
    String ERA_COMMONS_ID = "eRACommonsID";
    String PUBMED_ID = "pubmedID";
    String SCIENTIFIC_URL = "scientificURL";


    @SqlQuery("select * from researcher_property where userId = :userId")
    List<ResearcherProperty> findResearcherPropertiesByUser(@Bind("userId") Integer userId);

    @SqlQuery("select propertyValue from researcher_property where userId = :userId and propertyKey = 'completed'")
    String isProfileCompleted(@Bind("userId") Integer userId);

    @SqlBatch("insert into researcher_property (userId, propertyKey, propertyValue) values (:userId, :propertyKey, :propertyValue)")
    void insertAll(@BindBean Collection<ResearcherProperty> researcherProperties);

    @SqlUpdate("delete from researcher_property where  userId = :userId")
    void deleteAllPropertiesByUser(@Bind("userId") Integer userId);

    @SqlBatch("delete from researcher_property where userId = :userId and propertyKey = :propertyKey")
    void deletePropertiesByUserAndKey(@BindBean Collection<ResearcherProperty> researcherProperties);

    @SqlUpdate("delete from researcher_property where  userId = :userId and propertyKey IN (<propertyKeyList>)")
    void deletePropertyByUser(@BindList("propertyKeyList") List<String> propertyKeyList, @Bind("userId") Integer userId);

    @SqlQuery(value = "select * from researcher_property where " +
            "(propertyKey = '" + INSTITUTION + "' AND propertyValue != :institutionName) OR " +
            "(propertyKey = '" + ARE_YOU_PRINCIPAL_INVESTIGATOR + "' AND  propertyValue != :isThePI) OR " +
            "(propertyKey = '" + DO_YOU_HAVE_PI + "' AND  propertyValue != :havePI) OR " +
            "(propertyKey = '" + ERA_COMMONS_ID + "' AND  propertyValue != :eRACommonsID) OR " +
            "(propertyKey = '" + PUBMED_ID + "' AND  propertyValue != :pubmedID) OR " +
            "(propertyKey = '" + SCIENTIFIC_URL + "' AND  propertyValue != :scientificURL) " +
            " AND userId = :userId")
    List<ResearcherProperty> findResearcherProperties(@Bind("userId") Integer userId, @Bind("institutionName") String institutionName,@Bind("isThePI") String isThePI,
                                                      @Bind("havePI") String havePI, @Bind("eRACommonsID") String eRACommonsID, @Bind("pubmedID") String pubmedID, @Bind("scientificURL") String scientificURL);

    @SqlQuery("select propertyValue from researcher_property  where  userId = :userId and propertyKey = :propertyKey")
    String findPropertyValueByPK(@Bind("userId") Integer userId, @Bind("propertyKey") String propertyKey);

    @SqlQuery("select * from researcher_property where userId  in (<userIds>)")
    List<ResearcherProperty> findResearcherPropertiesByUserIds(@BindList("userIds") List<Integer> userIds);
}
