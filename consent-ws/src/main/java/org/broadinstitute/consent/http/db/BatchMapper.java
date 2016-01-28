package org.broadinstitute.consent.http.db;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class BatchMapper implements ResultSetMapper< Map<String,Integer>> {

    @Override
    public  Map<String,Integer> map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        Map<String,Integer> map = new HashMap<>();
        map.put(r.getString("objectId"),r.getInt("dataSetId"));
        return map;
    }

}



