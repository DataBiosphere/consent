package org.broadinstitute.consent.http.service.dao;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.CounterDAO;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.enumeration.DarDocumentType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Handle;
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
    return updateByReferenceId(user, dar, List.of("status")).dar();
  }

  /**
   * A draft edit's save: the saved DAR, and the document locations it removed that no DAR refers to
   * any more, for the caller to delete once the save has committed.
   */
  public record DraftSave(DataAccessRequest dar, List<String> removedDocuments) {}

  /**
   * Saves a draft's data. The keys in {@code keepFromRow} are server-managed: their values come
   * from the row being updated, not from {@code dar}, so a client can't set them and a change that
   * commits while this update waits on the row (a cancel, a document upload) is not overwritten.
   */
  public DraftSave updateByReferenceId(
      User user, DataAccessRequest dar, Collection<String> keepFromRow) {
    Instant now = Instant.now();
    String referenceId = dar.getReferenceId();
    final String updateDataByReferenceId =
        """
          UPDATE data_access_request
          SET data = (regexp_replace(:data, '\\\\u0000', '', 'g')::jsonb
                  - CAST(:keepFromRow AS text[]))
              || COALESCE(
                  (SELECT jsonb_object_agg(k, data -> k)
                   FROM unnest(CAST(:keepFromRow AS text[])) AS k
                   WHERE data -> k IS NOT NULL),
                  '{}'::jsonb),
            user_id = :userId,
          update_date = :updateDate
          WHERE reference_id = :referenceId AND submission_date IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM election e WHERE e.reference_id = data_access_request.reference_id)
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
    final String documentLocations =
        """
          SELECT data ->> 'irbDocumentLocation', data ->> 'collaborationLetterLocation'
          FROM data_access_request WHERE reference_id = :referenceId
        """;
    List<String> removedDocuments = new ArrayList<>();
    jdbi.useTransaction(
        h -> {
          // The row lock is taken here, so the locations read before and after are this edit's.
          List<String> before =
              readLocations(h, documentLocations + " FOR NO KEY UPDATE", referenceId);
          Update darUpdate = h.createUpdate(updateDataByReferenceId);
          darUpdate.bind("referenceId", referenceId);
          darUpdate.bind("userId", user.getUserId());
          darUpdate.bind("updateDate", now);
          darUpdate.bind("data", dar.getData().toString());
          darUpdate.bindArray("keepFromRow", String.class, List.copyOf(keepFromRow));
          // Only a draft with no elections can be edited. The caller's draft check ran on an
          // earlier read, and a submit may have committed since; the guard keeps an edit from
          // turning a submitted DAR back into a draft.
          if (darUpdate.execute() == 0) {
            throwWhyNotUpdated(h, referenceId);
          }
          checkNoElectionsUnderCollectionLock(h, dar, user, now);

          List<String> after = readLocations(h, documentLocations, referenceId);
          DataAccessRequestDAO darDAO = h.attach(DataAccessRequestDAO.class);
          before.stream()
              .filter(location -> !after.contains(location))
              .filter(location -> !darDAO.isDocumentReferenced(location))
              .forEach(removedDocuments::add);

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
    return new DraftSave(dataAccessRequestDAO.findByReferenceId(referenceId), removedDocuments);
  }

  /** The DAR's non-null document locations, as the query reads them. */
  private static List<String> readLocations(Handle h, String sql, String referenceId) {
    return h.createQuery(sql)
        .bind("referenceId", referenceId)
        .map(
            (rs, ctx) ->
                Stream.of(rs.getString(1), rs.getString(2)).filter(Objects::nonNull).toList())
        .findOne()
        .orElse(List.of());
  }

  /** A document upload's save: the location it replaced, if any, and the saved DAR. */
  public record DocumentSave(String previousLocation, DataAccessRequest dar) {}

  /**
   * Saves an uploaded document's location and name on a draft, and nothing else, so an edit or
   * another upload that commits meanwhile is not overwritten. Returns the location it replaced, as
   * read under the row lock, so the caller deletes the document the draft no longer refers to.
   */
  public DocumentSave saveDocument(User user, DataAccessRequest dar, DarDocumentType type) {
    Instant now = Instant.now();
    String referenceId = dar.getReferenceId();
    boolean irb = DarDocumentType.IRB.equals(type);
    String locationField = irb ? "irbDocumentLocation" : "collaborationLetterLocation";
    JsonObject patch = new JsonObject();
    patch.addProperty(
        locationField,
        irb
            ? dar.getData().getIrbDocumentLocation()
            : dar.getData().getCollaborationLetterLocation());
    patch.addProperty(
        irb ? "irbDocumentName" : "collaborationLetterName",
        irb ? dar.getData().getIrbDocumentName() : dar.getData().getCollaborationLetterName());
    final String saveDocument =
        """
          UPDATE data_access_request d
          SET data = d.data || CAST(:patch AS jsonb), update_date = :updateDate
          FROM (SELECT reference_id, data ->> :locationField AS previous
                FROM data_access_request WHERE reference_id = :referenceId FOR NO KEY UPDATE) old
          WHERE d.reference_id = old.reference_id AND d.submission_date IS NULL
            AND NOT EXISTS (SELECT 1 FROM election e WHERE e.reference_id = d.reference_id)
          RETURNING COALESCE(old.previous, '')
        """;
    // The saved DAR is read in the same transaction, so a failure after the commit can't lose the
    // location this save replaced.
    return jdbi.inTransaction(
        h -> {
          Optional<String> saved =
              h.createQuery(saveDocument)
                  .bind("patch", patch.toString())
                  .bind("updateDate", now)
                  .bind("locationField", locationField)
                  .bind("referenceId", referenceId)
                  .mapTo(String.class)
                  .findOne();
          if (saved.isEmpty()) {
            throwWhyNotUpdated(h, referenceId);
          }
          checkNoElectionsUnderCollectionLock(h, dar, user, now);
          return new DocumentSave(
              saved.get().isEmpty() ? null : saved.get(),
              h.attach(DataAccessRequestDAO.class).findByReferenceId(referenceId));
        });
  }

  /**
   * Only a draft with no elections can be changed. Explains why an update guarded on that matched
   * no row: the DAR is gone (404), submitted (422), or a draft with elections (409).
   */
  private static void throwWhyNotUpdated(Handle h, String referenceId) {
    boolean draft =
        h.createQuery(
                """
                  SELECT submission_date IS NULL FROM data_access_request
                  WHERE reference_id = :referenceId
                """)
            .bind("referenceId", referenceId)
            .mapTo(Boolean.class)
            .findOne()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Data access request %s not found".formatted(referenceId)));
    if (!draft) {
      throw new SubmittedDARCannotBeEditedException();
    }
    throw new ConsentConflictException("Elections present on this DAR; it cannot be edited");
  }

  /**
   * Takes the DAR's collection lock, under which elections are opened, and fails the change if one
   * committed while it waited.
   */
  private void checkNoElectionsUnderCollectionLock(
      Handle h, DataAccessRequest dar, User user, Instant now) {
    if (Objects.isNull(dar.getCollectionId())) {
      return;
    }
    darCollectionDAO.updateDarCollection(dar.getCollectionId(), user.getUserId(), Date.from(now));
    boolean elections =
        h.createQuery("SELECT EXISTS (SELECT 1 FROM election WHERE reference_id = :referenceId)")
            .bind("referenceId", dar.getReferenceId())
            .mapTo(Boolean.class)
            .one();
    if (elections) {
      throw new ConsentConflictException("Elections present on this DAR; it cannot be edited");
    }
  }
}
