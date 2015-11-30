package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.DataSet;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DataSetMapper implements ResultSetMapper<DataSet> {

    public DataSet map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new DataSet(r.getInt("dataSetId"), r.getString("objectId"), r.getString("name"), r.getTimestamp("createDate"), r.getBoolean("active"));
    }
}
