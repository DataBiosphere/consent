package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Dictionary;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DictionaryMapper implements RowMapper<Dictionary> {
    @Override
    public Dictionary map(ResultSet r, StatementContext statementContext) throws SQLException {
        return new Dictionary(
                r.getInt("key_id"),
                r.getString("key"),
                r.getBoolean("required"),
                r.getInt("display_order"),
                r.getInt("receive_order"));
    }
}
