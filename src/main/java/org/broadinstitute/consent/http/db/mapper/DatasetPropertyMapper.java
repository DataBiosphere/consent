package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetPropertyMapper implements RowMapper<DatasetProperty> {

    public DatasetProperty map(ResultSet r, StatementContext ctx) throws SQLException {
      return new DatasetProperty(
          r.getInt("propertyid"),
          r.getInt("datasetid"),
          r.getInt("propertykey"),
          r.getString("propertyvalue"),
          DatasetPropertyType.parse(r.getString("propertytype")),
          r.getTimestamp("createdate")
      );
    }
}
