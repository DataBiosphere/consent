package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonSyntaxException;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DataAccessRequestMapper implements RowMapper<DataAccessRequest>, RowMapperHelper {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public DataAccessRequest map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        DataAccessRequest dar = new DataAccessRequest();
        dar.setId(resultSet.getInt("id"));
        dar.setReferenceId(resultSet.getString("reference_id"));
        dar.setDraft(resultSet.getBoolean("draft"));
        String darDataString = resultSet.getObject("data", PGobject.class).getValue();
        // Handle nested quotes
        String quoteFixedDataString = darDataString.replaceAll("\\\\\"", "'");
        // Inserted json data ends up double-escaped via standard jdbi insert.
        String escapedDataString = unescapeJava(quoteFixedDataString);
        try {
            DataAccessRequestData data = DataAccessRequestData.fromString(escapedDataString);
            data.setReferenceId(dar.getReferenceId());
            dar.setData(data);
        } catch (JsonSyntaxException e) {
            String message = "Unable to parse Data Access Request, reference id: " + dar.getReferenceId() + "; error: " + e.getMessage();
            logger.error(message);
            throw new SQLException(message);
        }
        return dar;
    }

}
