package org.broadinstitute.consent.http.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.broadinstitute.consent.http.models.DataRequest;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class DataRequestMapper implements ResultSetMapper<DataRequest> {

    public DataRequest map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new DataRequest(r.getInt("requestId"), r.getInt("purposeId"), r.getString("description"), r.getString("researcher"), r.getInt("dataSetId"));
    }
}
