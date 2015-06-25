package org.genomebridge.consent.http.db;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

public interface DACUserDAO extends Transactional<DACUserDAO> {

    @SqlQuery("select * from dacuser where dacUserId = :dacUserId")
    String findDACUserById(@Bind("dacUserId") Integer dacUserId);
    
}
