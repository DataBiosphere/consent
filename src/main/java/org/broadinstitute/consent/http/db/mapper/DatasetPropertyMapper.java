package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.DataSetProperty;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetPropertyMapper implements RowMapper<DataSetProperty> {

    public DataSetProperty map(ResultSet r, StatementContext ctx) throws SQLException {
      return new DataSetProperty(
          r.getInt("propertyid"),
          r.getInt("datasetid"),
          r.getInt("propertykey"),
          r.getString("propertyvalue"),
          r.getTimestamp("createdate")
      );
    }
}
