package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.broadinstitute.consent.http.db.AdminDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class AdminDashboardDAOTest extends DAOTestHelper {

  private DashboardDatabaseCounts counts() {
    return jdbi.onDemand(AdminDashboardDAO.class).getCounts();
  }

  @Test
  void returnsZeroCountsWithoutData() {
    DashboardDatabaseCounts counts = counts();

    assertEquals(0, counts.darTotal());
    assertEquals(0, counts.dacs());
    assertEquals(0, counts.users());
    assertEquals(0, counts.libraryCards());
  }

  @Test
  void countsDarsAcrossInstitutionsByTheirStatus() {
    User first = createUserWithInstitution();
    User second = createUserWithInstitution();
    Integer firstDataset = createDataset(first);
    castFinalVote(
        first,
        createSubmittedDar(first, firstDataset, new DataAccessRequestData()),
        firstDataset,
        true);
    Integer secondDataset = createDataset(second);
    castFinalVote(
        second,
        createSubmittedDar(second, secondDataset, new DataAccessRequestData()),
        secondDataset,
        false);
    DataAccessRequestData canceled = new DataAccessRequestData();
    canceled.setStatus("Canceled");
    createSubmittedDar(second, createDataset(second), canceled);

    DashboardDatabaseCounts counts = counts();

    assertEquals(3, counts.darTotal());
    assertEquals(1, counts.darApproved());
    assertEquals(1, counts.darCanceled());
  }

  @Test
  void countsACollectionOnceFromItsLatestSubmission() {
    User user = createUserWithInstitution();
    Integer datasetId = createDataset(user);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    insertSubmittedDar(
        user, collectionId, datasetId, Date.from(Instant.parse("2026-01-01T00:00:00Z")));
    insertSubmittedDar(
        user, collectionId, datasetId, Date.from(Instant.parse("2026-02-01T00:00:00Z")));

    assertEquals(1, counts().darTotal());
  }

  @Test
  void excludesCollectionWhoseLatestSubmissionIsArchived() {
    User user = createUserWithInstitution();
    DataAccessRequestData archived = new DataAccessRequestData();
    archived.setStatus("Archived");
    createSubmittedDar(user, createDataset(user), archived);

    assertEquals(0, counts().darTotal());
  }

  @Test
  void countsDacsUsersAndLibraryCards() {
    User user = createUser();
    dacDAO.createDac("Broad DAC", "broad@example.org", "", user.getUserId());
    Integer deletedDacId = dacDAO.createDac("Old DAC", "old@example.org", "", user.getUserId());
    dacDAO.deleteDac(deletedDacId, user.getUserId());
    libraryCardDAO.insertLibraryCard(
        user.getUserId(), "name", user.getEmail(), user.getUserId(), FIXED_DATE);

    DashboardDatabaseCounts counts = counts();

    assertEquals(1, counts.dacs());
    assertEquals(1, counts.users());
    assertEquals(1, counts.libraryCards());
  }

  private Integer createDataset(User user) {
    return datasetDAO.insertDataset(
        "Dashboard dataset " + UUID.randomUUID(),
        FIXED_TIMESTAMP,
        user.getUserId(),
        UUID.randomUUID().toString(),
        "{}",
        null);
  }

  private void castFinalVote(User user, String referenceId, Integer datasetId, boolean approve) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CLOSED.getValue(),
            FIXED_DATE,
            referenceId,
            datasetId);
    Integer voteId = voteDAO.insertVote(user.getUserId(), electionId, VoteType.FINAL.getValue());
    updateVote(approve, "rationale", FIXED_DATE, voteId, false, electionId, FIXED_DATE, false);
  }

  private String createSubmittedDar(User user, Integer datasetId, DataAccessRequestData data) {
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        FIXED_DATE,
        FIXED_DATE,
        FIXED_DATE,
        data,
        "era-commons-id");
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    return referenceId;
  }

  private void insertSubmittedDar(
      User user, Integer collectionId, Integer datasetId, Date submissionDate) {
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        FIXED_DATE,
        submissionDate,
        FIXED_DATE,
        new DataAccessRequestData(),
        "era-commons-id");
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
  }
}
