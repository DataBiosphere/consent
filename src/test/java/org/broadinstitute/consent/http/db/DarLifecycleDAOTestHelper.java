package org.broadinstitute.consent.http.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;

/**
 * Fixtures for the DAR lifecycle as the app writes it: original DARs, progress reports and
 * closeouts filed against a parent, and data-access elections decided by a final vote.
 */
abstract class DarLifecycleDAOTestHelper extends DAOTestHelper {

  protected User user;

  @BeforeEach
  void createSubmitter() {
    user = createUserWithInstitution();
  }

  protected Integer createDataset() {
    return datasetDAO.insertDataset(
        "Dataset " + UUID.randomUUID(),
        FIXED_TIMESTAMP,
        user.getUserId(),
        UUID.randomUUID().toString(),
        "{}",
        null);
  }

  protected Integer createStudy() {
    return studyDAO.insertStudy(
        "Study " + UUID.randomUUID(),
        "Description",
        "PI",
        null,
        List.of("Genomic"),
        true,
        user.getUserId(),
        Instant.now(),
        UUID.randomUUID());
  }

  protected Integer createStudyDataset(Integer studyId) {
    Integer datasetId = createDataset();
    datasetDAO.updateStudyId(datasetId, studyId);
    return datasetId;
  }

  /** An original DAR in a collection of its own. */
  protected String createDar(Instant submitted, Integer... datasetIds) {
    return createDar(new DataAccessRequestData(), submitted, datasetIds);
  }

  protected String createDar(DataAccessRequestData data, Instant submitted, Integer... datasetIds) {
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), Date.from(submitted));
    return createDarIn(collectionId, data, submitted, datasetIds);
  }

  /**
   * Another original DAR in a sibling's collection, as submissions were saved before 2022-07-27.
   */
  protected String createSiblingDar(String siblingReferenceId, Instant submitted, Integer... ids) {
    Integer collectionId =
        dataAccessRequestDAO.findByReferenceId(siblingReferenceId).getCollectionId();
    return createDarIn(collectionId, new DataAccessRequestData(), submitted, ids);
  }

  /** An original DAR with no collection, as some legacy rows are. */
  protected String createDarWithoutCollection(Instant submitted, Integer... datasetIds) {
    return createDarIn(null, new DataAccessRequestData(), submitted, datasetIds);
  }

  private String createDarIn(
      Integer collectionId, DataAccessRequestData data, Instant submitted, Integer... datasetIds) {
    Date on = Date.from(submitted);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), on, on, on, data, "era");
    for (Integer datasetId : datasetIds) {
      dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    }
    return referenceId;
  }

  protected String createProgressReport(
      String parentReferenceId, Instant on, Integer... datasetIds) {
    return createProgressReport(parentReferenceId, new DataAccessRequestData(), on, datasetIds);
  }

  protected String createProgressReport(
      String parentReferenceId, DataAccessRequestData data, Instant on, Integer... datasetIds) {
    String referenceId = createChild(parentReferenceId, data, on);
    for (Integer datasetId : datasetIds) {
      dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    }
    return referenceId;
  }

  protected String createCloseout(String parentReferenceId, Instant on) {
    return createChild(parentReferenceId, closeoutData(), on);
  }

  protected DataAccessRequestData closeoutData() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(
        new CloseoutSupplement(List.of("Research complete"), null, user.getUserId()));
    return data;
  }

  private String createChild(String parentReferenceId, DataAccessRequestData data, Instant on) {
    var parent = dataAccessRequestDAO.findByReferenceId(parentReferenceId);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        parent.getId(), parent.getCollectionId(), referenceId, user.getUserId(), data, "era");
    // insertProgressReport submits at now(); the fixtures need the date the test names
    jdbi.useHandle(
        h ->
            h.createUpdate(
                    "UPDATE data_access_request SET submission_date = :on"
                        + " WHERE reference_id = :ref")
                .bind("on", Timestamp.from(on))
                .bind("ref", referenceId)
                .execute());
    return referenceId;
  }

  protected Integer election(String referenceId, Integer datasetId, ElectionStatus status) {
    return electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(), status.getValue(), new Date(), referenceId, datasetId);
  }

  /** Closes a new election with a final vote cast {@code on}; null leaves the vote undated. */
  protected void decide(String referenceId, Integer datasetId, boolean vote, Instant on) {
    decide(referenceId, datasetId, VoteType.FINAL, vote, on);
  }

  protected void decide(
      String referenceId, Integer datasetId, VoteType type, boolean vote, Instant on) {
    castVote(election(referenceId, datasetId, ElectionStatus.CLOSED), type, vote, on);
  }

  /** Casts a vote {@code on} in an existing election; null leaves the vote undated. */
  protected void castVote(Integer electionId, VoteType type, boolean vote, Instant on) {
    Integer voteId = voteDAO.insertVote(user.getUserId(), electionId, type.getValue());
    Date cast = on == null ? null : Date.from(on);
    updateVote(vote, "", cast, voteId, false, electionId, new Date(), false);
  }

  protected void approve(String referenceId, Integer datasetId, Instant on) {
    decide(referenceId, datasetId, true, on);
  }
}
