package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.broadinstitute.consent.http.db.AdminDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
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
    assertEquals(0, counts.darApproved());
    assertEquals(0, counts.darCanceled());
    assertEquals(0, counts.dacs());
    assertEquals(0, counts.users());
    assertEquals(0, counts.institutions());
    assertEquals(0, counts.libraryCards());
    assertEquals(0, counts.agreements());
    assertEquals(0, counts.institutionsWithoutSigningOfficial());
    assertEquals(0, counts.researchersApproved());
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
    DataAccessRequestData canceled = new DataAccessRequestData();
    canceled.setStatus("Canceled");
    String original =
        insertSubmittedDar(
            user,
            collectionId,
            datasetId,
            Date.from(Instant.parse("2026-01-01T00:00:00Z")),
            canceled);
    insertProgressReport(user, collectionId, original, datasetId, new DataAccessRequestData());

    DashboardDatabaseCounts counts = counts();

    assertEquals(1, counts.darTotal());
    assertEquals(0, counts.darCanceled());
  }

  @Test
  void countsADarApprovedOnlyWhenEveryDatasetIsApproved() {
    User user = createUserWithInstitution();
    Integer first = createDataset(user);
    Integer second = createDataset(user);
    String referenceId = createSubmittedDar(user, first, new DataAccessRequestData());
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, second);
    castFinalVote(user, referenceId, first, true);

    assertEquals(0, counts().darApproved());

    castVote(user, referenceId, second, true, VoteType.RADAR_APPROVE, FIXED_DATE);

    assertEquals(1, counts().darApproved());
  }

  @Test
  void decidesADatasetByItsNewestVote() {
    User user = createUserWithInstitution();
    Integer datasetId = createDataset(user);
    String referenceId = createSubmittedDar(user, datasetId, new DataAccessRequestData());
    castVote(
        user,
        referenceId,
        datasetId,
        true,
        VoteType.FINAL,
        Date.from(Instant.parse("2026-01-01T00:00:00Z")));
    castVote(
        user,
        referenceId,
        datasetId,
        false,
        VoteType.FINAL,
        Date.from(Instant.parse("2026-02-01T00:00:00Z")));

    assertEquals(0, counts().darApproved());
  }

  @Test
  void decidesAPre2022SubmissionByEveryOriginalDar() {
    User user = createUserWithInstitution();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    Integer denied = createDataset(user);
    Integer approved = createDataset(user);
    castFinalVote(
        user,
        insertSubmittedDar(
            user, collectionId, denied, Date.from(Instant.parse("2020-01-01T00:00:00Z"))),
        denied,
        false);
    castFinalVote(
        user,
        insertSubmittedDar(
            user, collectionId, approved, Date.from(Instant.parse("2020-01-01T00:00:05Z"))),
        approved,
        true);

    DashboardDatabaseCounts counts = counts();

    assertEquals(1, counts.darTotal());
    assertEquals(0, counts.darApproved());
  }

  @Test
  void ignoresACanceledOriginalDarOfAPre2022Submission() {
    User user = createUserWithInstitution();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    Integer approved = createDataset(user);
    castFinalVote(
        user, insertSubmittedDar(user, collectionId, approved, FIXED_DATE), approved, true);
    DataAccessRequestData canceled = new DataAccessRequestData();
    canceled.setStatus("Canceled");
    insertSubmittedDar(user, collectionId, createDataset(user), FIXED_DATE, canceled);

    DashboardDatabaseCounts counts = counts();

    assertEquals(1, counts.darTotal());
    assertEquals(1, counts.darApproved());
    assertEquals(0, counts.darCanceled());
  }

  @Test
  void excludesCollectionWhoseLatestSubmissionIsArchived() {
    User user = createUserWithInstitution();
    Integer datasetId = createDataset(user);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    DataAccessRequestData archived = new DataAccessRequestData();
    archived.setStatus("Archived");
    String original =
        insertSubmittedDar(
            user, collectionId, datasetId, Date.from(Instant.parse("2026-01-01T00:00:00Z")));
    insertProgressReport(user, collectionId, original, datasetId, archived);

    // Filtering archived rows before picking the latest would count the original submission.
    assertEquals(0, counts().darTotal());
  }

  @Test
  void countsInstitutionsWithoutASigningOfficial() {
    User signingOfficial = createUserWithInstitution();
    for (String name : new String[] {"No SO One", "No SO Two"}) {
      Integer institutionId = insertInstitution(name, signingOfficial.getUserId());
      createUserWithRole(UserRoles.RESEARCHER.getRoleId(), institutionId);
    }

    DashboardDatabaseCounts counts = counts();

    assertEquals(3, counts.institutions());
    assertEquals(2, counts.institutionsWithoutSigningOfficial());
  }

  @Test
  void countsDacsUsersLibraryCardsAndDaaAssociations() {
    User user = createUser();
    Integer dacId = dacDAO.createDac("Broad DAC", "broad@example.org", "", user.getUserId());
    Integer deletedDacId = dacDAO.createDac("Old DAC", "old@example.org", "", user.getUserId());
    Integer deletedDaaId =
        daaDAO.createDaa(
            user.getUserId(), Instant.now(), user.getUserId(), Instant.now(), deletedDacId);
    daaDAO.createDacDaaRelation(deletedDacId, deletedDaaId, user.getUserId());
    dacDAO.deleteDac(deletedDacId, user.getUserId());
    Integer daaId =
        daaDAO.createDaa(user.getUserId(), Instant.now(), user.getUserId(), Instant.now(), dacId);
    daaDAO.createDacDaaRelation(dacId, daaId, user.getUserId());
    Integer cardId =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(), "name", user.getEmail(), user.getUserId(), FIXED_DATE);
    libraryCardDAO.createLibraryCardDaaRelation(user.getUserId(), user.getUserId(), cardId, daaId);

    DashboardDatabaseCounts counts = counts();

    assertEquals(1, counts.dacs());
    assertEquals(1, counts.users());
    assertEquals(1, counts.libraryCards());
    assertEquals(1, counts.agreements());
    assertEquals(1, counts.researchersApproved());
  }

  private Integer insertInstitution(String name, Integer createUserId) {
    return institutionDAO.insertInstitution(
        name,
        "itDirectorName",
        "itDirectorEmail",
        null,
        null,
        null,
        null,
        null,
        null,
        createUserId,
        FIXED_DATE);
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
    castVote(user, referenceId, datasetId, approve, VoteType.FINAL, FIXED_DATE);
  }

  private void castVote(
      User user,
      String referenceId,
      Integer datasetId,
      boolean approve,
      VoteType type,
      Date castDate) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CLOSED.getValue(),
            FIXED_DATE,
            referenceId,
            datasetId);
    Integer voteId = voteDAO.insertVote(user.getUserId(), electionId, type.getValue());
    updateVote(approve, "rationale", castDate, voteId, false, electionId, castDate, false);
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

  private String insertSubmittedDar(
      User user, Integer collectionId, Integer datasetId, Date submissionDate) {
    return insertSubmittedDar(
        user, collectionId, datasetId, submissionDate, new DataAccessRequestData());
  }

  private String insertSubmittedDar(
      User user,
      Integer collectionId,
      Integer datasetId,
      Date submissionDate,
      DataAccessRequestData data) {
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        FIXED_DATE,
        submissionDate,
        FIXED_DATE,
        data,
        "era-commons-id");
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    return referenceId;
  }

  private void insertProgressReport(
      User user,
      Integer collectionId,
      String parentReferenceId,
      Integer datasetId,
      DataAccessRequestData data) {
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        dataAccessRequestDAO.findByReferenceId(parentReferenceId).getId(),
        collectionId,
        referenceId,
        user.getUserId(),
        data,
        "era-commons-id");
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
  }
}
