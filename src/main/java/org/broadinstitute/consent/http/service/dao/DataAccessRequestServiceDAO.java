package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

public class DataAccessRequestServiceDAO {

  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DarCollectionDAO darCollectionDAO;
  private final Jdbi jdbi;

  @Inject
  public DataAccessRequestServiceDAO(DataAccessRequestDAO dataAccessRequestDAO, Jdbi jdbi,
      DarCollectionDAO darCollectionDAO) {
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.jdbi = jdbi;
    this.darCollectionDAO = darCollectionDAO;
  }

  public DataAccessRequest updateByReferenceId(User user, DataAccessRequest dar)
      throws SQLException {
    Instant now = Instant.now();
    String referenceId = dar.getReferenceId();
    jdbi.useHandle(handle -> {
      handle.getConnection().setAutoCommit(false);
      handle.useTransaction(h -> {

        final String updateDataByReferenceId = "UPDATE data_access_request "
            + "SET data = to_jsonb(regexp_replace(:data, '\\\\u0000', '', 'g')), user_id = :userId, sort_date = :sortDate, "
            + "submission_date = :submissionDate, update_date = :updateDate "
            + "WHERE reference_id = :referenceId";
        final String deleteDarDatasetRelationByReferenceId = "DELETE FROM dar_dataset WHERE reference_id = :referenceId";
        final String insertDarDataset = "INSERT INTO dar_dataset (reference_id, dataset_id) "
            + "VALUES (:referenceId, :datasetId) "
            + "ON CONFLICT DO NOTHING";

        Update darUpdate = h.createUpdate(updateDataByReferenceId);
        darUpdate.bind("referenceId", referenceId);
        darUpdate.bind("userId", user.getUserId());
        darUpdate.bind("sortDate", now);
        darUpdate.bind("updateDate", now);
        darUpdate.bind("data", dar.getData().toString());
        darUpdate.bind("submissionDate", dar.getSubmissionDate());
        darUpdate.execute();

        if (Objects.nonNull(dar.getCollectionId())) {
          darCollectionDAO.updateDarCollection(dar.getCollectionId(), user.getUserId(),
              new Date(now.getEpochSecond()));
        }

        Update darDatasetDelete = h.createUpdate(deleteDarDatasetRelationByReferenceId);
        darDatasetDelete.bind("referenceId", dar.getReferenceId());
        darDatasetDelete.execute();

        List<Integer> datasetIds = dar.getDatasetIds();
        datasetIds.forEach(id -> {
          Update darDatasetInsert = h.createUpdate(insertDarDataset);
          darDatasetInsert.bind("referenceId", referenceId);
          darDatasetInsert.bind("datasetId", id);
          darDatasetInsert.execute();
        });

        h.commit();
      });
    });
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }
}
