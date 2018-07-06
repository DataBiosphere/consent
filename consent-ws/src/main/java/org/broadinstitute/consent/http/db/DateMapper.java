package org.broadinstitute.consent.http.db;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class DateMapper implements ResultSetMapper<Date> {

    @Override
    public Date map(int i, ResultSet r, StatementContext statementContext) throws SQLException {
        return r.getDate("createDate");
    }
}
