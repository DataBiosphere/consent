package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.db.mapper.UserFileMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

@RegisterRowMapper(UserFileMapper.class)
public interface UserFileDAO extends Transactional<InstitutionDAO> {

    @SqlUpdate(
        "INSERT INTO file "
    )
    void insertNewFile();

}
