package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.postgresql.util.PGobject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DataAccessRequestDataMapper implements RowMapper<DataAccessRequestData>, RowMapperHelper {


    @Override
    public DataAccessRequestData map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        String darDataString = resultSet.getObject("data", PGobject.class).getValue();
        DataAccessRequestData data = translate(darDataString);
        return data;
    }

}
