package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonSyntaxException;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class DataAccessRequestDataMapper implements RowMapper<DataAccessRequestData>, RowMapperHelper {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public DataAccessRequestData map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        DataAccessRequestData data = null;
        String darDataString = resultSet.getObject("data", PGobject.class).getValue();
        if (Objects.nonNull(darDataString)) {
            // Handle nested quotes
            String quoteFixedDataString = darDataString.replaceAll("\\\\\"", "'");
            // Inserted json data ends up double-escaped via standard jdbi insert.
            String escapedDataString = unescapeJava(quoteFixedDataString);
            try {
                data = DataAccessRequestData.fromString(escapedDataString);
            } catch (JsonSyntaxException | NullPointerException e) {
                String message = "Unable to parse Data Access Request; error: " + e.getMessage();
                logger.error(message);
                throw new SQLException(message);
            }
        }
        return data;
    }

}
