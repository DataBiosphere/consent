package org.broadinstitute.consent.http.db;


import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface AssociationDAO {

    @SqlQuery("select ca.associationId from consentassociations ca inner join dataset ds on ds.dataSetId = ca.dataSetId" +
              " where ca.associationType = :associationType and ds.objectId = :objectId")
    Integer findAssociationIdByTypeAndObjectId(@Bind("associationType") String associationType,
                                                @Bind("objectId") String objectId);
}
