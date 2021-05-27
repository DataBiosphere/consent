package org.broadinstitute.consent.http.db.mapper;

import com.google.gson.JsonSyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataAccessRequestMapper implements RowMapper<DataAccessRequest>, RowMapperHelper {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public DataAccessRequest map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        DataAccessRequest dar = new DataAccessRequest();
        dar.setId(resultSet.getInt("id"));
        dar.setReferenceId(resultSet.getString("reference_id"));
        dar.setDraft(resultSet.getBoolean("draft"));
        dar.setUserId(resultSet.getInt("user_id"));
        dar.setCreateDate(resultSet.getTimestamp("create_date"));
        dar.setSortDate(resultSet.getTimestamp("sort_date"));
        dar.setSubmissionDate(resultSet.getTimestamp("submission_date"));
        dar.setUpdateDate(resultSet.getTimestamp("update_date"));

        //for dars with the entire data object
        if (hasColumn(resultSet, "data")) {
            String darDataString = resultSet.getObject("data", PGobject.class).getValue();
            // Handle nested quotes
            String quoteFixedDataString = darDataString.replaceAll("\\\\\"", "'");
            // Inserted json data ends up double-escaped via standard jdbi insert.
            String escapedDataString = unescapeJava(quoteFixedDataString);
            try {
                DataAccessRequestData data = DataAccessRequestData.fromString(escapedDataString);
                data.setReferenceId(dar.getReferenceId());
                dar.setData(data);
            } catch (JsonSyntaxException | NullPointerException e) {
                String message = "Unable to parse Data Access Request, reference id: " + dar.getReferenceId() + "; error: " + e.getMessage();
                logger.error(message);
                throw new SQLException(message);
            }

        //for dars the do not contain the entire data object but do contain individual fields from the data object
        //the data object must be manually constructed, fields can be added here if needed as more DAO calls are created
        } else if (hasColumn(resultSet, "project_title")) {
            DataAccessRequestData data = new DataAccessRequestData();

            //different dars have different names for this field
            String darCode = resultSet.getString("darCode");
            if (Objects.isNull(darCode)) {
                darCode = resultSet.getString("dar_code");
            }
            //different dars have different names for this field
            String nonTechRus = resultSet.getString("nonTechRus");
            if (Objects.isNull(nonTechRus)) {
                nonTechRus = resultSet.getString("non_tech_rus");
            }
            data.setDarCode(darCode);
            data.setNonTechRus(nonTechRus);
            data.setProjectTitle(resultSet.getString("project_title"));
            data.setInvestigator(resultSet.getString("investigator"));
            dar.setData(data);
        }
        return dar;
    }

}
