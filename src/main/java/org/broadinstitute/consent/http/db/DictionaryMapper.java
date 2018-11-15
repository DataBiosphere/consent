package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.Dictionary;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DictionaryMapper implements ResultSetMapper<Dictionary> {
    @Override
    public Dictionary map(int i, ResultSet r, StatementContext statementContext) throws SQLException {
        return new Dictionary(r.getInt("keyId"), r.getString("key"), r.getBoolean("required"), r.getInt("displayOrder"), r.getInt("receiveOrder"));
    }
}
