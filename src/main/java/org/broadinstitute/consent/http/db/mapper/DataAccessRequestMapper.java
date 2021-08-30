package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.postgresql.util.PGobject;

public class DataAccessRequestMapper implements RowMapper<DataAccessRequest>, RowMapperHelper {

    @Override
    public DataAccessRequest map(ResultSet resultSet, StatementContext statementContext) throws SQLException {
        DataAccessRequest dar = new DataAccessRequest();
        dar.setId(resultSet.getInt("id"));
        dar.setReferenceId(resultSet.getString("reference_id"));
        if (hasColumn(resultSet, "collection_id")) {
            dar.setCollectionId(resultSet.getInt("collection_id"));
        }
        dar.setDraft(resultSet.getBoolean("draft"));
        dar.setUserId(resultSet.getInt("user_id"));
        dar.setCreateDate(resultSet.getTimestamp("create_date"));
        dar.setSortDate(resultSet.getTimestamp("sort_date"));
        dar.setSubmissionDate(resultSet.getTimestamp("submission_date"));
        dar.setUpdateDate(resultSet.getTimestamp("update_date"));
        String darDataString = resultSet.getObject("data", PGobject.class).getValue();
        DataAccessRequestData data = RowMapperHelper.translate(darDataString);
        dar.setData(data);
        return dar;
    }

}
