package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DataAccessRequestMapper implements RowMapper<DataAccessRequest> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public DataAccessRequest map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        DataAccessRequest dar = new DataAccessRequest();
        dar.setId(resultSet.getInt("id"));
        dar.setReferenceId(resultSet.getString("reference_id"));
        String darDataString = resultSet.getObject("data", PGobject.class).getValue();
        try {
            DataAccessRequestData data = DataAccessRequestData.fromString(escapeStoredJson(darDataString));
            dar.setData(data);
        } catch (JsonSyntaxException e) {
            String message = "Unable to parse Data Access Request, reference id: " + dar.getReferenceId() + "; error: " + e.getMessage();
            logger.error(message);
            throw new SQLException(message);
        }
        return dar;
    }

    /**
     * Stored jsonb data in postgres requires a bit of processing to make it Gson-friendly.
     * TODO: move this to RowMapperHelper once that is merged
     * @param json Unescaped json
     * @return Escaped json
     */
    private String escapeStoredJson(String json) {
        return StringUtils.
                stripEnd(StringUtils.stripStart(json, "\""), "\""). // Remove surrounding " literals
                replace("\\\"", "\""). // Remove all of the prepended \" literals surrounding string keys and values
                replace("\\\\\"", "'"); // Replace \\" literals with single quotes
    }

}
