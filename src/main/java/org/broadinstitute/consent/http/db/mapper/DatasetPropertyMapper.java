package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DatasetPropertyMapper implements RowMapper<DatasetProperty>, RowMapperHelper {

    public DatasetProperty map(ResultSet r, StatementContext ctx) throws SQLException {
        DatasetProperty prop = new DatasetProperty(
                r.getInt("property_id"),
                r.getInt("dataset_id"),
                r.getInt("property_key"),
                r.getString("schema_property"),
                r.getString("property_value"),
                PropertyType.parse(r.getString("property_type")),
                r.getTimestamp("create_date")
        );
        if (hasColumn(r, "key")) {
            prop.setPropertyName(r.getString("key"));
        }
        return prop;
    }
}
