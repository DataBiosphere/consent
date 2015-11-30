package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.DataRequest;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

@RegisterMapper({DataRequestMapper.class})
public interface DataRequestDAO extends Transactional<DataRequestDAO> {

    @SqlQuery("select * from datarequest where requestId = :requestId")
    DataRequest findDataRequestById(@Bind("requestId") Integer requestId);

    @SqlUpdate("insert into datarequest " +
            "(purposeId, description, researcher, dataSetId) values " +
            "( :purposeId, :description, :researcher, :dataSetId)")
    @GetGeneratedKeys
    Integer insertDataRequest(@Bind("purposeId") Integer purposeId,
                              @Bind("description") String description,
                              @Bind("researcher") String researcher,
                              @Bind("dataSetId") Integer dataSetId);

    @SqlUpdate("update datarequest set dataSetId = :dataSetId, description = :description, "
            + " purposeId = :purposeId, researcher = :researcher where requestId = :requestId ")
    void updateDataRequest(@Bind("purposeId") Integer purposeId,
                           @Bind("dataSetId") Integer dataSetId,
                           @Bind("description") String description,
                           @Bind("researcher") String researcher,
                           @Bind("requestId") Integer requestId);

    @SqlUpdate("delete  from datarequest where requestId = :requestId")
    void deleteDataRequestById(@Bind("requestId") Integer requestId);

}       
