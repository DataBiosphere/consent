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
  public DataAccessRequest map(ResultSet resultSet, StatementContext statementContext)
      throws SQLException {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setId(resultSet.getInt("id"));
    dar.setReferenceId(resultSet.getString("reference_id"));
    if (hasNonZeroColumn(resultSet, "collection_id")) {
      int collectionId = resultSet.getInt("collection_id");
      if (!resultSet.wasNull()) {
        dar.setCollectionId(collectionId);
      }
    }
    if (hasColumn(resultSet, "dar_code")) {
      dar.setDarCode(resultSet.getString("dar_code"));
    }

    if (hasNonZeroColumn(resultSet, "parent_id")) {
      dar.setParentId(resultSet.getInt("parent_id"));
    }

    if (hasNonZeroColumn(resultSet, "user_id")) {
      dar.setUserId(resultSet.getInt("user_id"));
    }

    dar.setCreateDate(resultSet.getTimestamp("create_date"));
    dar.setSubmissionDate(resultSet.getTimestamp("submission_date"));
    dar.setUpdateDate(resultSet.getTimestamp("update_date"));
    String darDataString = resultSet.getObject("data", PGobject.class).getValue();
    DataAccessRequestData data = translate(darDataString);
    if (hasNonZeroColumn(resultSet, "dataset_id")) {
      dar.addDatasetId(resultSet.getInt("dataset_id"));
    }
    dar.setData(data);
    dar.setEraCommonsId(resultSet.getString("era_commons_id"));
    dar.setCloseoutSigningOfficialApprovedDate(
        resultSet.getTimestamp("closeout_so_approval_timestamp"));
    if (hasNonZeroColumn(resultSet, "closeout_approving_so_id")) {
      dar.setCloseoutSigningOfficialApprovedUserId(resultSet.getInt("closeout_approving_so_id"));
    }

    dar.setApprovingSigningOfficialApprovedDate(resultSet.getTimestamp("approving_so_timestamp"));
    if (hasNonZeroColumn(resultSet, "approving_so_id")) {
      dar.setApprovingSigningOfficialUserId(resultSet.getInt("approving_so_id"));
    }

    dar.setRequiresSOApproval(resultSet.getBoolean("requires_so_approval"));

    return dar;
  }
}
