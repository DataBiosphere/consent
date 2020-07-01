package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Counter;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CounterMapper implements RowMapper<Counter> {

    @Override
    public Counter map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new Counter(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("count")
        );
    }
}
