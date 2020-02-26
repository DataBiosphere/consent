package org.broadinstitute.consent.http.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class DateMapper implements RowMapper<Date> {

    @Override
    public Date map(ResultSet r, StatementContext statementContext) throws SQLException {
        return r.getDate("createDate");
    }
}
