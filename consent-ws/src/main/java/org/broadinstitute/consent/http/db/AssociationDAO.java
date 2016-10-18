package org.broadinstitute.consent.http.db;


import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;


public interface AssociationDAO {

    @SqlQuery("select associationId from consentassociations where associationType = :associationType and objectId = :objectId")
    Integer findAssociationIdByTypeAndObjectId(@Bind("associationType") String associationType,
                                                @Bind("objectId") String objectId);
}
