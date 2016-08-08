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

    @SqlQuery("select * from researcher_property where userId = :userId")
    List<ResearcherProperty> findResearcherPropertiesByUser(@Bind("userId") Integer userId);

    @SqlBatch("insert into researcher_property (userId, propertyKey, propertyValue) values (:userId, :propertyKey, :propertyValue)")
    void insertAll(@BindBean Collection<ResearcherProperty> researcherProperties);


    @SqlBatch("update researcher_property set propertyValue = :propertyValue where userId = :userId and propertyKey = :propertyKey")
    void updateAll(@BindBean Collection<ResearcherProperty> researcherProperties);

    @SqlUpdate("delete from researcher_property where  userId = :userId")
    void deleteAllPropertiesByUser(@Bind("userId") Integer userId);
}