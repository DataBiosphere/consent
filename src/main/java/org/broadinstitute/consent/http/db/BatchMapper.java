package org.broadinstitute.consent.http.db;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class BatchMapper implements RowMapper< Map<String,Integer>> {

    @Override
    public  Map<String,Integer> map(ResultSet r, StatementContext ctx) throws SQLException {
        Map<String,Integer> map = new HashMap<>();
        map.put(r.getString("name"),r.getInt("dataSetId"));
        return map;
    }

}



