package org.genomebridge.consent.http.db;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

@RegisterMapper({DataRequestMapper.class})
public interface DataRequestDAO extends Transactional<DataRequestDAO> {

    @SqlQuery("select * from datarequest where requestId = :requestId")
    String checkDataRequestbyId(@Bind("requestId") Integer requestId);

}
