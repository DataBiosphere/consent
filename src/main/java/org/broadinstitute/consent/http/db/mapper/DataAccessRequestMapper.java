package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DataAccessRequestMapper implements RowMapper<DataAccessRequest> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final Gson gson = new Gson();

    @Override
    public DataAccessRequest map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        DataAccessRequest dar = new DataAccessRequest();
        dar.setId(resultSet.getInt("id"));
        dar.setReferenceId(resultSet.getString("reference_id"));
        try {
            dar.setData(gson.fromJson(resultSet.getString("data"), DataAccessRequestData.class));
        } catch (JsonSyntaxException e) {
            logger.error("Unable to parse Data Access Request, id: " + dar.getId() + "; error: " + e.getMessage());
        }
        return dar;
    }

}
