package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.db.DacDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DacDashboardDAOTest extends DAOTestHelper {

  private final Map<String, Integer> datasetIdsByReferenceId = new HashMap<>();

  @Test
  void returnsZeroCountsWithoutDacsOrRequests() {
    DashboardDatabaseCounts counts =
        jdbi.onDemand(DacDashboardDAO.class)
            .getCounts(Integer.MAX_VALUE, UserRoles.CHAIRPERSON.getRoleId(), true);

    assertEquals(0, counts.dacs());
    assertEquals(0, counts.darTotal());
    assertEquals(0, counts.darApproved());
    assertEquals(0, counts.awaitingMyVote());
  }

  @Test
  void countsChairRequestsByCompletionAndVoteAction() {
    User owner = createUser();
    Integer dacId = createDac(owner);
    User chair = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dacId);
    User researcher = createUser();

    String completed = createSubmittedDar(researcher, createDataset(owner, dacId), false);
    createElection(completed, datasetIdFor(completed), ElectionStatus.CLOSED);

    String awaitingVote = createSubmittedDar(researcher, createDataset(owner, dacId), false);
    createElection(awaitingVote, datasetIdFor(awaitingVote), ElectionStatus.OPEN);

    createSubmittedDar(researcher, createDataset(owner, dacId), false);

    DashboardDatabaseCounts counts = getCounts(chair, UserRoles.CHAIRPERSON, true);

    assertEquals(1, counts.dacs());
    assertEquals(3, counts.darTotal());
    assertEquals(1, counts.darApproved());
    assertEquals(1, counts.awaitingMyVote());
  }

  @Test
  void doesNotOfferChairVoteActionForCloseout() {
    User owner = createUser();
    Integer dacId = createDac(owner);
    User chair = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dacId);
    User researcher = createUser();
    String closeout = createSubmittedDar(researcher, createDataset(owner, dacId), true);
    createElection(closeout, datasetIdFor(closeout), ElectionStatus.OPEN);

    DashboardDatabaseCounts counts = getCounts(chair, UserRoles.CHAIRPERSON, true);

    assertEquals(1, counts.darTotal());
    assertEquals(0, counts.awaitingMyVote());
  }

  @Test
  void countsOnlyMemberRequestsWithAPendingDacVote() {
    User owner = createUser();
    Integer dacId = createDac(owner);
    User member = createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dacId);
    User researcher = createUser();

    String pendingVote = createSubmittedDar(researcher, createDataset(owner, dacId), false);
    Integer pendingElection =
        createElection(pendingVote, datasetIdFor(pendingVote), ElectionStatus.OPEN);
    voteDAO.insertVote(member.getUserId(), pendingElection, VoteType.DAC.getValue());

    String noVote = createSubmittedDar(researcher, createDataset(owner, dacId), false);
    createElection(noVote, datasetIdFor(noVote), ElectionStatus.OPEN);

    DashboardDatabaseCounts counts = getCounts(member, UserRoles.MEMBER, false);

    assertEquals(2, counts.darTotal());
    assertEquals(0, counts.darApproved());
    assertEquals(1, counts.awaitingMyVote());
  }

  @Test
  void scopesRequestsToTheSelectedRoleAndUsesTheLatestElection() {
    User owner = createUser();
    Integer chairDacId = createDac(owner);
    Integer memberDacId = createDac(owner);
    User user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), chairDacId);
    dacDAO.addDacMember(
        UserRoles.MEMBER.getRoleId(), user.getUserId(), memberDacId, user.getUserId());
    User researcher = createUser();

    String chairDar = createSubmittedDar(researcher, createDataset(owner, chairDacId), false);
    Integer chairDataset = datasetIdFor(chairDar);
    createElection(chairDar, chairDataset, ElectionStatus.CLOSED);
    createElection(chairDar, chairDataset, ElectionStatus.OPEN);

    createSubmittedDar(researcher, createDataset(owner, memberDacId), false);

    DashboardDatabaseCounts chairCounts = getCounts(user, UserRoles.CHAIRPERSON, true);
    DashboardDatabaseCounts memberCounts = getCounts(user, UserRoles.MEMBER, false);

    assertEquals(1, chairCounts.darTotal());
    assertEquals(0, chairCounts.darApproved());
    assertEquals(1, chairCounts.awaitingMyVote());
    assertEquals(1, memberCounts.darTotal());
  }

  private DashboardDatabaseCounts getCounts(User user, UserRoles role, boolean isChair) {
    return jdbi.onDemand(DacDashboardDAO.class)
        .getCounts(user.getUserId(), role.getRoleId(), isChair);
  }

  private Integer createDac(User owner) {
    return dacDAO.createDac(
        "Dashboard DAC " + UUID.randomUUID(),
        UUID.randomUUID() + "@example.org",
        "Dashboard test DAC",
        owner.getUserId());
  }

  private Integer createDataset(User owner, Integer dacId) {
    return datasetDAO.insertDataset(
        "Dashboard dataset " + UUID.randomUUID(),
        FIXED_TIMESTAMP,
        owner.getUserId(),
        UUID.randomUUID().toString(),
        EMPTY_JSON_DOCUMENT,
        dacId);
  }

  private String createSubmittedDar(User researcher, Integer datasetId, boolean closeout) {
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), researcher.getUserId(), FIXED_DATE);
    String referenceId = UUID.randomUUID().toString();
    DataAccessRequestData data = new DataAccessRequestData();
    if (closeout) {
      data.setCloseoutSupplement(
          new CloseoutSupplement(
              List.of("Project completed"), "Synthetic closeout notes", researcher.getUserId()));
    }
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        researcher.getUserId(),
        FIXED_DATE,
        Date.from(Instant.now()),
        FIXED_DATE,
        data,
        "synthetic-era-id");
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    datasetIdsByReferenceId.put(referenceId, datasetId);
    return referenceId;
  }

  private Integer datasetIdFor(String referenceId) {
    return datasetIdsByReferenceId.get(referenceId);
  }

  private Integer createElection(String referenceId, Integer datasetId, ElectionStatus status) {
    return electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(), status.getValue(), FIXED_DATE, referenceId, datasetId);
  }
}
