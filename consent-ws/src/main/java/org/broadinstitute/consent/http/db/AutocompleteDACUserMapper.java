package org.broadinstitute.consent.http.db;


import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AutocompleteDACUserMapper implements ResultSetMapper<Map<String, String>> {

    public Map<String, String> map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        Map<String, String> j = new HashMap<>();
        j.put("email", r.getString("email"));
        j.put("displayName", r.getString("displayName"));
      return j;
    }
}