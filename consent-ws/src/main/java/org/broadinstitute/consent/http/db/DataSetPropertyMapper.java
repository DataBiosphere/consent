package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.DataSetProperty;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataSetPropertyMapper implements ResultSetMapper<DataSetProperty> {

    @Override
    public DataSetProperty map(int index, ResultSet r, StatementContext statementContext) throws SQLException {
        return new DataSetProperty(r.getInt("propertyId"), r.getInt("dataSetId"), r.getInt("propertyKey"), r.getString("propertyValue"), r.getDate("createDate"));
    }
}
