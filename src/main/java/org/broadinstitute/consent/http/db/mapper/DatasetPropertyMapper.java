package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetPropertyMapper implements RowMapper<DatasetProperty>, RowMapperHelper {

    public DatasetProperty map(ResultSet r, StatementContext ctx) throws SQLException {
        DatasetProperty prop = new DatasetProperty(
          r.getInt("property_id"),
          r.getInt("dataset_id"),
          r.getInt("property_key"),
          r.getString("schema_property"),
          r.getString("property_value"),
          DatasetPropertyType.parse(r.getString("property_type")),
          r.getTimestamp("create_date")
      );
      if (hasColumn(r, "key")) {
          prop.setPropertyName(r.getString("key"));
      }
      return prop;
    }
}
