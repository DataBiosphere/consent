package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

import java.util.Collection;
import java.util.List;

@UseStringTemplate3StatementLocator
@RegisterMapper({ResearcherPropertyMapper.class})
public interface ResearcherPropertyDAO extends Transactional<ResearcherPropertyDAO> {

    public static final String INSTITUTION = "institution";
    public  static final String ARE_YOU_PRINCIPAL_INVESTIGATOR = "isThePI";
    public static final String DO_YOU_HAVE_PI = "havePI";
    public static final String ERA_COMMONS_ID = "eRACommonsID";
    public static final String PUBMED_ID = "pubmedID";
    public static final String SCIENTIFIC_URL = "scientificURL";


    @SqlQuery("select * from researcher_property where userId = :userId")
    List<ResearcherProperty> findResearcherPropertiesByUser(@Bind("userId") Integer userId);

    @SqlQuery("select propertyValue from researcher_property where userId = :userId and propertyKey = 'completed'")
    String isProfileCompleted(@Bind("userId") Integer userId);

    @SqlBatch("insert into researcher_property (userId, propertyKey, propertyValue) values (:userId, :propertyKey, :propertyValue)")
    void insertAll(@BindBean Collection<ResearcherProperty> researcherProperties);

    @SqlUpdate("delete from researcher_property where  userId = :userId")
    void deleteAllPropertiesByUser(@Bind("userId") Integer userId);

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
}