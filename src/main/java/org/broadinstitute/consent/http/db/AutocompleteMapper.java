package org.broadinstitute.consent.http.db;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class AutocompleteMapper implements RowMapper<Map<String, String>> {

    public Map<String, String> map(ResultSet r, StatementContext ctx) throws SQLException {
        Map<String, String> j = new HashMap<>();
        j.put("id", r.getString("id"));
        j.put("objectId", r.getString("objId"));
        j.put("concatenation", r.getString("concatenation"));
      return j;
    }
}