package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.db.ResearcherDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ResearcherDashboardDAOTest extends DAOTestHelper {

  /** The one-year window {@code DataAccessRequest.EXPIRATION_DURATION_MILLIS} defines. */
  private static final int EXPIRATION_DAYS = 365;

  private static final int EXPIRING_SOON_DAYS = 30;

  private DashboardDatabaseCounts getCounts(User user) {
    return jdbi.onDemand(ResearcherDashboardDAO.class)
        .getCounts(user.getUserId(), EXPIRATION_DAYS, EXPIRING_SOON_DAYS);
  }

  @Test
  void returnsZeroCountsForResearcherWithoutData() {
    DashboardDatabaseCounts counts =
        jdbi.onDemand(ResearcherDashboardDAO.class)
            .getCounts(Integer.MAX_VALUE, EXPIRATION_DAYS, EXPIRING_SOON_DAYS);

    assertEquals(0, counts.darTotal());
    assertEquals(0, counts.darApproved());
    assertEquals(0, counts.darCanceled());
    assertEquals(0, counts.approvalsActive());
    assertEquals(0, counts.approvalsExpiringSoon());
    assertEquals(0, counts.approvalsExpired());
  }

  @Test
  void countsOnlyTheResearchersOwnRequests() {
    User user = createUser();
    User otherResearcher = createUser();
    createSubmittedDar(user, createDataset(user), new DataAccessRequestData(), FIXED_DATE);
    createSubmittedDar(
        otherResearcher, createDataset(otherResearcher), new DataAccessRequestData(), FIXED_DATE);

    assertEquals(1, getCounts(user).darTotal());
  }

  @Test
  void countsCanceledDarAsCanceledRatherThanInProcess() {
    User user = createUser();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus("Canceled");
    createSubmittedDar(user, createDataset(user), data, FIXED_DATE);

    DashboardDatabaseCounts counts = getCounts(user);

    assertEquals(1, counts.darTotal());
    assertEquals(0, counts.darApproved());
    assertEquals(1, counts.darCanceled());
  }

  @ParameterizedTest
  @EnumSource(
      value = VoteType.class,
      names = {"FINAL", "RADAR_APPROVE"})
  void countsDarAsApprovedWhenEveryDatasetHasPositiveFinalVote(VoteType voteType) {
    User user = createUser();
    Integer datasetId = createDataset(user);
    String referenceId =
        createSubmittedDar(user, datasetId, new DataAccessRequestData(), FIXED_DATE);
    approve(user, referenceId, datasetId, voteType, true);

    assertEquals(1, getCounts(user).darApproved());
  }

  @Test
  void countsDarVotedDownAsInProcessBecauseThereIsNoDeniedStatus() {
    User user = createUser();
    Integer datasetId = createDataset(user);
    String referenceId =
        createSubmittedDar(user, datasetId, new DataAccessRequestData(), FIXED_DATE);
    approve(user, referenceId, datasetId, VoteType.FINAL, false);

    DashboardDatabaseCounts counts = getCounts(user);

    // The DAR Requests page has no denied status: a voted-down request is neither approved nor
    // canceled, so it stays In Process.
    assertEquals(1, counts.darTotal());
    assertEquals(0, counts.darApproved());
    assertEquals(0, counts.darCanceled());
  }

  @Test
  void excludesCollectionWhoseLatestSubmissionIsArchived() {
    User user = createUser();
    Integer datasetId = createDataset(user);
    Integer collectionId = createCollection(user);
    insertSubmittedDar(
        user,
        collectionId,
        datasetId,
        new DataAccessRequestData(),
        Date.from(Instant.parse("2026-01-01T00:00:00Z")));
    DataAccessRequestData archived = new DataAccessRequestData();
    archived.setStatus("Archived");
    insertSubmittedDar(
        user, collectionId, datasetId, archived, Date.from(Instant.parse("2026-02-01T00:00:00Z")));

    assertEquals(0, getCounts(user).darTotal());
  }

  @Test
  void countsApprovalsOlderThanAYearAsExpiredInsteadOfDroppingThem() {
    User user = createUser();
    giveLibraryCard(user);
    Integer expiredDataset = createDataset(user);
    Integer activeDataset = createDataset(user);
    Integer expiringSoonDataset = createDataset(user);
    approveDatasetSubmittedDaysAgo(user, expiredDataset, EXPIRATION_DAYS + 10);
    approveDatasetSubmittedDaysAgo(user, activeDataset, 5);
    approveDatasetSubmittedDaysAgo(user, expiringSoonDataset, EXPIRATION_DAYS - 10);

    DashboardDatabaseCounts counts = getCounts(user);

    assertEquals(1, counts.approvalsExpired());
    // Expiring soon is a subset of active, as the tile reads it.
    assertEquals(2, counts.approvalsActive());
    assertEquals(1, counts.approvalsExpiringSoon());
  }

  @Test
  void doesNotCountDatasetsWithoutAnApprovingVote() {
    User user = createUser();
    giveLibraryCard(user);
    Integer datasetId = createDataset(user);
    String referenceId =
        createSubmittedDar(user, datasetId, new DataAccessRequestData(), FIXED_DATE);
    approve(user, referenceId, datasetId, VoteType.FINAL, false);

    DashboardDatabaseCounts counts = getCounts(user);

    assertEquals(0, counts.approvalsActive());
    assertEquals(0, counts.approvalsExpired());
  }

  @Test
  void doesNotCountApprovalsFromAClosedOutCollection() {
    User user = createUser();
    giveLibraryCard(user);
    Integer datasetId = createDataset(user);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(
        new CloseoutSupplement(List.of("Project completed"), "Closeout notes", user.getUserId()));
    String referenceId = createSubmittedDar(user, datasetId, data, recentDate(5));
    approve(user, referenceId, datasetId, VoteType.FINAL, true);

    assertEquals(0, getCounts(user).approvalsActive());
  }

  @Test
  void doesNotCountApprovalsForResearcherWithoutALibraryCard() {
    User user = createUser();
    Integer datasetId = createDataset(user);
    String referenceId =
        createSubmittedDar(user, datasetId, new DataAccessRequestData(), recentDate(5));
    approve(user, referenceId, datasetId, VoteType.FINAL, true);

    assertEquals(0, getCounts(user).approvalsActive());
  }

  /**
   * The dashboard's Active count and the My Dataset Approvals page must agree, so these compare
   * this DAO against the query that page renders from.
   */
  private long pageRowCount(User user) {
    return datasetDAO.getApprovedDatasets(user.getUserId()).size();
  }

  @Test
  void activeCountMatchesTheApprovalsPageForSeparateCollections() {
    User user = createUser();
    giveLibraryCard(user);
    approveDatasetSubmittedDaysAgo(user, createDataset(user), 5);
    approveDatasetSubmittedDaysAgo(user, createDataset(user), 30);

    assertEquals(pageRowCount(user), getCounts(user).approvalsActive());
    assertEquals(2, getCounts(user).approvalsActive());
  }

  @Test
  void activeCountMatchesTheApprovalsPageWhenACollectionHasSeveralSubmissions() {
    User user = createUser();
    giveLibraryCard(user);
    Integer datasetId = createDataset(user);
    Integer collectionId = createCollection(user);
    // A revised DAR: two submissions of the same collection, both approved for the same dataset.
    // The page reduces them to one row, keyed by dar_code and dataset.
    String first =
        insertSubmittedDar(
            user, collectionId, datasetId, new DataAccessRequestData(), recentDate(90));
    approve(user, first, datasetId, VoteType.FINAL, true);
    String second =
        insertSubmittedDar(
            user, collectionId, datasetId, new DataAccessRequestData(), recentDate(5));
    approve(user, second, datasetId, VoteType.FINAL, true);

    assertEquals(pageRowCount(user), getCounts(user).approvalsActive());
    assertEquals(1, getCounts(user).approvalsActive());
  }

  @Test
  void countsTheSameDatasetOnceForEachCollectionThatApprovedIt() {
    User user = createUser();
    giveLibraryCard(user);
    Integer datasetId = createDataset(user);
    approveDatasetSubmittedDaysAgo(user, datasetId, 5);
    approveDatasetSubmittedDaysAgo(user, datasetId, 40);

    assertEquals(pageRowCount(user), getCounts(user).approvalsActive());
    assertEquals(2, getCounts(user).approvalsActive());
  }

  @Test
  void datesARevisedApprovalFromItsLatestSubmission() {
    User user = createUser();
    giveLibraryCard(user);
    Integer datasetId = createDataset(user);
    Integer collectionId = createCollection(user);
    // The first submission is already past the one-year window; the revision is not, so the
    // approval is still active.
    String stale =
        insertSubmittedDar(
            user,
            collectionId,
            datasetId,
            new DataAccessRequestData(),
            recentDate(EXPIRATION_DAYS + 10));
    approve(user, stale, datasetId, VoteType.FINAL, true);
    String revised =
        insertSubmittedDar(
            user, collectionId, datasetId, new DataAccessRequestData(), recentDate(5));
    approve(user, revised, datasetId, VoteType.FINAL, true);

    DashboardDatabaseCounts counts = getCounts(user);

    assertEquals(1, counts.approvalsActive());
    assertEquals(0, counts.approvalsExpired());
    assertEquals(pageRowCount(user), counts.approvalsActive());
  }

  @Test
  void agreesWithTheApprovalsPageWhenVoteCreateAndUpdateOrderDisagree() {
    User user = createUser();
    giveLibraryCard(user);
    Integer datasetId = createDataset(user);
    String referenceId =
        createSubmittedDar(user, datasetId, new DataAccessRequestData(), recentDate(5));
    // A re-opened election: the vote created first was edited last. Ranking by create_date and
    // ranking by update_date therefore pick different votes.
    Date early = Date.from(Instant.now().minus(10, ChronoUnit.DAYS));
    Date late = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    Integer firstElection =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CLOSED.getValue(),
            FIXED_DATE,
            referenceId,
            datasetId);
    Integer firstVote =
        voteDAO.insertVote(user.getUserId(), firstElection, VoteType.FINAL.getValue());
    updateVote(false, "not approved", late, firstVote, false, firstElection, early, false);
    Integer secondElection =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CLOSED.getValue(),
            FIXED_DATE,
            referenceId,
            datasetId);
    Integer secondVote =
        voteDAO.insertVote(user.getUserId(), secondElection, VoteType.FINAL.getValue());
    updateVote(true, "approved", early, secondVote, false, secondElection, late, false);

    assertEquals(pageRowCount(user), getCounts(user).approvalsActive());
  }

  @Test
  void activeCountMatchesTheApprovalsPageAcrossAMixedFixture() {
    User user = createUser();
    giveLibraryCard(user);

    // Two straightforward approvals in separate collections.
    approveDatasetSubmittedDaysAgo(user, createDataset(user), 5);
    approveDatasetSubmittedDaysAgo(user, createDataset(user), 60);

    // Two datasets approved on one DAR.
    Integer sharedDarFirst = createDataset(user);
    Integer sharedDarSecond = createDataset(user);
    String shared =
        createSubmittedDar(user, sharedDarFirst, new DataAccessRequestData(), recentDate(10));
    dataAccessRequestDAO.insertDARDatasetRelation(shared, sharedDarSecond);
    approve(user, shared, sharedDarFirst, VoteType.FINAL, true);
    approve(user, shared, sharedDarSecond, VoteType.FINAL, true);

    // A revised DAR: one approval across two submissions.
    Integer revisedDataset = createDataset(user);
    Integer revisedCollection = createCollection(user);
    String firstSubmission =
        insertSubmittedDar(
            user, revisedCollection, revisedDataset, new DataAccessRequestData(), recentDate(80));
    approve(user, firstSubmission, revisedDataset, VoteType.FINAL, true);
    String revision =
        insertSubmittedDar(
            user, revisedCollection, revisedDataset, new DataAccessRequestData(), recentDate(3));
    approve(user, revision, revisedDataset, VoteType.FINAL, true);

    // Excluded by both: voted down, closed out, and another researcher's approval.
    Integer deniedDataset = createDataset(user);
    String denied =
        createSubmittedDar(user, deniedDataset, new DataAccessRequestData(), recentDate(5));
    approve(user, denied, deniedDataset, VoteType.FINAL, false);
    Integer closedOutDataset = createDataset(user);
    DataAccessRequestData closedOut = new DataAccessRequestData();
    closedOut.setCloseoutSupplement(
        new CloseoutSupplement(List.of("Project completed"), "Closeout notes", user.getUserId()));
    String closedOutDar = createSubmittedDar(user, closedOutDataset, closedOut, recentDate(5));
    approve(user, closedOutDar, closedOutDataset, VoteType.FINAL, true);
    User otherResearcher = createUser();
    giveLibraryCard(otherResearcher);
    approveDatasetSubmittedDaysAgo(otherResearcher, createDataset(otherResearcher), 5);

    // Counted only by the dashboard: the approvals page filters expired approvals out entirely.
    approveDatasetSubmittedDaysAgo(user, createDataset(user), EXPIRATION_DAYS + 20);

    DashboardDatabaseCounts counts = getCounts(user);

    assertEquals(pageRowCount(user), counts.approvalsActive());
    assertEquals(5, counts.approvalsActive());
    assertEquals(1, counts.approvalsExpired());
  }

  private void approveDatasetSubmittedDaysAgo(User user, Integer datasetId, int daysAgo) {
    String referenceId =
        createSubmittedDar(user, datasetId, new DataAccessRequestData(), recentDate(daysAgo));
    approve(user, referenceId, datasetId, VoteType.FINAL, true);
  }

  private void approve(
      User user, String referenceId, Integer datasetId, VoteType voteType, boolean approved) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CLOSED.getValue(),
            FIXED_DATE,
            referenceId,
            datasetId);
    Integer voteId = voteDAO.insertVote(user.getUserId(), electionId, voteType.getValue());
    updateVote(
        approved,
        approved ? "approved" : "not approved",
        FIXED_DATE,
        voteId,
        false,
        electionId,
        FIXED_DATE,
        false);
  }

  private static Date recentDate(int daysAgo) {
    return Date.from(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
  }

  private void giveLibraryCard(User user) {
    libraryCardDAO.insertLibraryCard(
        user.getUserId(), user.getDisplayName(), user.getEmail(), user.getUserId(), FIXED_DATE);
  }

  private Integer createDataset(User user) {
    Integer dacId =
        dacDAO.createDac(
            "DAC " + UUID.randomUUID(),
            randomAlphabetic(10) + "@example.org",
            "",
            user.getUserId());
    return datasetDAO.insertDataset(
        "Dashboard dataset " + UUID.randomUUID(),
        FIXED_TIMESTAMP,
        user.getUserId(),
        UUID.randomUUID().toString(),
        EMPTY_JSON_DOCUMENT,
        dacId);
  }

  private Integer createCollection(User user) {
    return darCollectionDAO.insertDarCollection(
        "DAR-" + UUID.randomUUID(), user.getUserId(), FIXED_DATE);
  }

  private String createSubmittedDar(
      User user, Integer datasetId, DataAccessRequestData data, Date submissionDate) {
    return insertSubmittedDar(user, createCollection(user), datasetId, data, submissionDate);
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
}
