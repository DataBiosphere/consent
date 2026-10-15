package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.db.SigningOfficialDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SigningOfficialDashboardDAOTest extends DAOTestHelper {

  @Test
  void returnsZeroCountsForInstitutionWithoutData() {
    SigningOfficialDashboardDAO dashboardDAO = jdbi.onDemand(SigningOfficialDashboardDAO.class);

    DashboardDatabaseCounts counts =
        dashboardDAO.getCounts(Integer.MAX_VALUE, "1", "so@example.org");

    assertEquals(0, counts.activeResearchers());
    assertEquals(0, counts.inactiveResearchers());
    assertEquals(0, counts.darTotal());
    assertEquals(0, counts.approvedDataSubmitters());
    assertEquals(0, counts.agreements());
    assertEquals(0, counts.researchersApproved());
  }

  @Test
  void countsCanceledDarAsCanceledRatherThanInProcess() {
    User user = createUserWithInstitution();
    Integer datasetId =
        datasetDAO.insertDataset(
            "Canceled DAR dataset",
            FIXED_TIMESTAMP,
            user.getUserId(),
            UUID.randomUUID().toString(),
            "{}",
            null);
    Integer collectionId =
        darCollectionDAO.insertDarCollection("DAR-CANCELED", user.getUserId(), FIXED_DATE);
    String referenceId = UUID.randomUUID().toString();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus("Canceled");
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

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(user.getInstitutionId(), user.getUserId().toString(), user.getEmail());

    assertEquals(1, counts.darTotal());
    assertEquals(0, counts.darApproved());
    assertEquals(1, counts.darCanceled());
  }

  @ParameterizedTest
  @EnumSource(
      value = VoteType.class,
      names = {"FINAL", "RADAR_APPROVE"})
  void countsDarAsApprovedWhenEveryDatasetHasPositiveFinalVote(VoteType voteType) {
    User user = createUserWithInstitution();
    Integer datasetId = createDataset(user);
    String referenceId = createSubmittedDar(user, datasetId, new DataAccessRequestData());
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CLOSED.getValue(),
            FIXED_DATE,
            referenceId,
            datasetId);
    Integer voteId = voteDAO.insertVote(user.getUserId(), electionId, voteType.getValue());
    updateVote(true, "approved", FIXED_DATE, voteId, false, electionId, FIXED_DATE, false);

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(user.getInstitutionId(), user.getUserId().toString(), user.getEmail());

    assertEquals(1, counts.darApproved());
  }

  @Test
  void doesNotCountDarAsApprovedWhenFinalVoteIsNegative() {
    User user = createUserWithInstitution();
    Integer datasetId = createDataset(user);
    String referenceId = createSubmittedDar(user, datasetId, new DataAccessRequestData());
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CLOSED.getValue(),
            FIXED_DATE,
            referenceId,
            datasetId);
    Integer voteId = voteDAO.insertVote(user.getUserId(), electionId, VoteType.FINAL.getValue());
    updateVote(false, "denied", FIXED_DATE, voteId, false, electionId, FIXED_DATE, false);

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(user.getInstitutionId(), user.getUserId().toString(), user.getEmail());

    // A DAR voted down is not approved and was not canceled, so it lands in In Process - the same
    // bucket the SO DAR Requests page puts it in, since that page has no denied status either.
    assertEquals(1, counts.darTotal());
    assertEquals(0, counts.darApproved());
    assertEquals(0, counts.darCanceled());
  }

  @Test
  void excludesCollectionWhoseLatestSubmissionIsArchived() {
    User user = createUserWithInstitution();
    Integer datasetId = createDataset(user);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    Date earlier = Date.from(Instant.parse("2026-01-01T00:00:00Z"));
    String original =
        insertSubmittedDar(user, collectionId, datasetId, new DataAccessRequestData(), earlier);
    DataAccessRequestData archived = new DataAccessRequestData();
    archived.setStatus("Archived");
    insertProgressReport(user, collectionId, original, datasetId, archived);

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(user.getInstitutionId(), user.getUserId().toString(), user.getEmail());

    assertEquals(0, counts.darTotal());
  }

  @Test
  void decidesAPre2022SubmissionByEveryOriginalDar() {
    User user = createUserWithInstitution();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    Integer denied = createDataset(user);
    Integer approved = createDataset(user);
    // The denied sibling still awaits this SO's approval.
    DataAccessRequestData needsSo = new DataAccessRequestData();
    needsSo.setSigningOfficialEmail(user.getEmail());
    String deniedDar =
        insertSubmittedDar(
            user, collectionId, denied, needsSo, Date.from(Instant.parse("2020-01-01T00:00:00Z")));
    dataAccessRequestDAO.updateRequiresSOApproval(true, deniedDar);
    castFinalVote(user, deniedDar, denied, false);
    castFinalVote(
        user,
        insertSubmittedDar(
            user,
            collectionId,
            approved,
            new DataAccessRequestData(),
            Date.from(Instant.parse("2020-01-01T00:00:05Z"))),
        approved,
        true);

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(user.getInstitutionId(), user.getUserId().toString(), user.getEmail());

    assertEquals(1, counts.darTotal());
    assertEquals(0, counts.darApproved());
    assertEquals(1, counts.approvalTotal());
    assertEquals(1, counts.awaitingSoAction());
  }

  @Test
  void ignoresACanceledOriginalDarOfAPre2022Submission() {
    User user = createUserWithInstitution();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    Integer approved = createDataset(user);
    castFinalVote(
        user,
        insertSubmittedDar(user, collectionId, approved, new DataAccessRequestData(), FIXED_DATE),
        approved,
        true);
    DataAccessRequestData canceled = new DataAccessRequestData();
    canceled.setStatus("Canceled");
    insertSubmittedDar(user, collectionId, createDataset(user), canceled, FIXED_DATE);

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(user.getInstitutionId(), user.getUserId().toString(), user.getEmail());

    assertEquals(1, counts.darTotal());
    assertEquals(1, counts.darApproved());
    assertEquals(0, counts.darCanceled());
  }

  @Test
  void countsApprovalTotalRegardlessOfWhetherTheSoAlreadyActioned() {
    User user = createUserWithInstitution();
    Integer datasetId = createDataset(user);
    DataAccessRequestData actioned = new DataAccessRequestData();
    actioned.setCloseoutSupplement(
        new CloseoutSupplement(List.of("Project completed"), "Closeout notes", user.getUserId()));
    String referenceId = fileCloseout(user, datasetId, actioned);
    dataAccessRequestDAO.updateDarApprovalSO(user.getUserId(), referenceId);

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(user.getInstitutionId(), user.getUserId().toString(), user.getEmail());

    assertEquals(1, counts.approvalTotal());
    assertEquals(0, counts.awaitingSoAction());
  }

  @Test
  void supplementOnOriginalDarCreatesNoSoWork() {
    User user = createUserWithInstitution();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(
        new CloseoutSupplement(List.of("Project completed"), "Closeout notes", user.getUserId()));
    createSubmittedDar(user, createDataset(user), data);

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(user.getInstitutionId(), user.getUserId().toString(), user.getEmail());

    assertEquals(0, counts.approvalTotal());
    assertEquals(0, counts.awaitingSoAction());
  }

  @Test
  void countsPendingCloseoutReviewAsAwaitingSoAction() {
    User user = createUserWithInstitution();
    Integer datasetId = createDataset(user);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(
        new CloseoutSupplement(List.of("Project completed"), "Closeout notes", user.getUserId()));
    fileCloseout(user, datasetId, data);

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(user.getInstitutionId(), user.getUserId().toString(), user.getEmail());

    assertEquals(1, counts.approvalTotal());
    assertEquals(1, counts.awaitingSoAction());
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

  private String createSubmittedDar(User user, Integer datasetId, DataAccessRequestData data) {
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    return insertSubmittedDar(user, collectionId, datasetId, data, FIXED_DATE);
  }

  private String insertSubmittedDar(
      User user,
      Integer collectionId,
      Integer datasetId,
      DataAccessRequestData data,
      Date submissionDate) {
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        FIXED_DATE, // createDate
        submissionDate,
        FIXED_DATE, // updateDate
        data,
        "era-commons-id");
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    return referenceId;
  }

  /** A closeout is filed as a progress report on an earlier original DAR, as in production. */
  private String fileCloseout(User user, Integer datasetId, DataAccessRequestData closeout) {
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
    String original =
        insertSubmittedDar(user, collectionId, datasetId, new DataAccessRequestData(), FIXED_DATE);
    return insertProgressReport(user, collectionId, original, datasetId, closeout);
  }

  private String insertProgressReport(
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
    return referenceId;
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

  @Test
  void doesNotCountDaaThatIsNotMappedToAnyDac() {
    User user = createUser();
    dacDAO.createDac("Broad DAC", "broad@example.org", "", user.getUserId());
    Integer otherDacId = dacDAO.createDac("Other DAC", "other@example.org", "", user.getUserId());
    daaDAO.createDaa(user.getUserId(), Instant.now(), user.getUserId(), Instant.now(), otherDacId);

    DashboardDatabaseCounts counts =
        jdbi.onDemand(SigningOfficialDashboardDAO.class)
            .getCounts(Integer.MAX_VALUE, user.getUserId().toString(), user.getEmail());

    assertEquals(0, counts.agreements());
  }
}
