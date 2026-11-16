package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.broadinstitute.consent.http.db.CounterDAO;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Update;

public class DataAccessRequestServiceDAO {

  public record TransactionDAOs(
      DataAccessRequestDAO dataAccessRequestDAO,
      DaaDAO daaDAO,
      DarCollectionDAO darCollectionDAO,
      CounterDAO counterDAO,
      ElectionDAO electionDAO) {}

  private final Jdbi jdbi;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DarCollectionDAO darCollectionDAO;

  @Inject
  public DataAccessRequestServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
    this.dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
    this.darCollectionDAO = jdbi.onDemand(DarCollectionDAO.class);
  }

  /**
   * Runs a data access request workflow with every participating DAO attached to the same handle.
   * Any exception from the callback rolls back all database changes made by the workflow.
   */
  public <T> T inTransaction(Function<TransactionDAOs, T> callback) {
    return jdbi.inTransaction(
        handle ->
            callback.apply(
                new TransactionDAOs(
                    handle.attach(DataAccessRequestDAO.class),
                    handle.attach(DaaDAO.class),
                    handle.attach(DarCollectionDAO.class),
                    handle.attach(CounterDAO.class),
                    handle.attach(ElectionDAO.class))));
  }

  public DataAccessRequest updateByReferenceId(User user, DataAccessRequest dar) {
    Instant now = Instant.now();
    String referenceId = dar.getReferenceId();
    final String updateDataByReferenceId =
        """
          UPDATE data_access_request
          SET data = regexp_replace(:data, '\\\\u0000', '', 'g')::jsonb, user_id = :userId,
          update_date = :updateDate
          WHERE reference_id = :referenceId AND submission_date IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM election e WHERE e.reference_id = data_access_request.reference_id)
        """;
    final String hasElections =
        """
          SELECT EXISTS (SELECT 1 FROM election WHERE reference_id = :referenceId)
        """;
    final String exists =
        """
          SELECT EXISTS (SELECT 1 FROM data_access_request WHERE reference_id = :referenceId)
        """;
    final String deleteDarDatasetRelationByReferenceId =
        """
          DELETE FROM dar_dataset WHERE reference_id = :referenceId
        """;
    final String insertDarDataset =
        """
          INSERT INTO dar_dataset (reference_id, dataset_id)
          VALUES (:referenceId, :datasetId)
          ON CONFLICT DO NOTHING
        """;
    jdbi.useTransaction(
        h -> {
          Update darUpdate = h.createUpdate(updateDataByReferenceId);
          darUpdate.bind("referenceId", referenceId);
          darUpdate.bind("userId", user.getUserId());
          darUpdate.bind("updateDate", now);
          darUpdate.bind("data", dar.getData().toString());
          // Only a draft with no elections can be edited. The caller's draft check ran on an
          // earlier read, and a submit may have committed since; the guard keeps an edit from
          // turning a submitted DAR back into a draft.
          if (darUpdate.execute() == 0) {
            if (!h.createQuery(exists)
                .bind("referenceId", referenceId)
                .mapTo(Boolean.class)
                .one()) {
              throw new NotFoundException(
                  "Data access request %s not found".formatted(referenceId));
            }
            if (h.createQuery(hasElections)
                .bind("referenceId", referenceId)
                .mapTo(Boolean.class)
                .one()) {
              throw new ConsentConflictException(
                  "Elections present on this DAR; it cannot be edited");
            }
            throw new SubmittedDARCannotBeEditedException();
          }

          if (Objects.nonNull(dar.getCollectionId())) {
            darCollectionDAO.updateDarCollection(
                dar.getCollectionId(), user.getUserId(), Date.from(now));
          }

          Update darDatasetDelete = h.createUpdate(deleteDarDatasetRelationByReferenceId);
          darDatasetDelete.bind("referenceId", dar.getReferenceId());
          darDatasetDelete.execute();

          List<Integer> datasetIds = dar.getDatasetIds();
          datasetIds.forEach(
              id -> {
                Update darDatasetInsert = h.createUpdate(insertDarDataset);
                darDatasetInsert.bind("referenceId", referenceId);
                darDatasetInsert.bind("datasetId", id);
                darDatasetInsert.execute();
              });
        });
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }
}
