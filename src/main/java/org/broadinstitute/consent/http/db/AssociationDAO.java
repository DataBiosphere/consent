package org.broadinstitute.consent.http.db;


import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;


public interface AssociationDAO {

    @SqlQuery("select ca.associationId from consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId" +
              " where ca.associationType = :associationType and ds.objectId = :objectId")
    Integer findAssociationIdByTypeAndObjectId(@Bind("associationType") String associationType,
                                                @Bind("objectId") String objectId);
}
