package org.genomebridge.consent.http.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.genomebridge.consent.http.models.DataSet;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class DataSetMapper implements ResultSetMapper<DataSet> {

    public DataSet map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new DataSet(r.getInt("dataSetId"), r.getString("objectId"), r.getString("name"), r.getDate("createDate"), r.getBoolean("active"));
    }
}
