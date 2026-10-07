package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.ElectionBucket;
import org.broadinstitute.consent.http.models.VoteBucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElectionMetricsDAOTest extends DAOTestHelper {

  private static final Instant FROM = startOf(LocalDate.of(2026, 1, 1));
  private static final Instant TO = startOf(LocalDate.of(2027, 1, 1));
  private static final Instant Q1 = FROM;
  private static final Instant Q2 = startOf(LocalDate.of(2026, 4, 1));
  private static final String LEGACY_RP_TYPE = "RP";

  private ElectionMetricsDAO dao;
  private Integer userId;
  private Integer datasetId;

  @BeforeEach
  void setUpDao() {
    dao = jdbi.onDemand(ElectionMetricsDAO.class);
    userId = createUser().getUserId();
    datasetId = dataset(null);
  }

  @Test
  void bucketsElectionsByStatusEitherSideOfAQuarterBoundary() {
    election(ElectionStatus.CLOSED, LocalDateTime.of(2026, 3, 31, 23, 0));
    election(ElectionStatus.OPEN, LocalDateTime.of(2026, 4, 1, 1, 0));
    election(ElectionStatus.OPEN, LocalDateTime.of(2026, 4, 2, 1, 0));
    election(ElectionStatus.CANCELED, LocalDateTime.of(2026, 4, 3, 1, 0));

    assertEquals(
        List.of(
            new ElectionBucket(Q1, ElectionStatus.CLOSED.getValue(), 1),
            new ElectionBucket(Q2, ElectionStatus.CANCELED.getValue(), 1),
            new ElectionBucket(Q2, ElectionStatus.OPEN.getValue(), 2)),
        dao.countElectionsOpened(FROM, TO, null, "quarter"));
  }

  @Test
  void countsOnlyDataAccessElectionsInsideTheRange() {
    election(ElectionStatus.OPEN, LocalDateTime.of(2025, 12, 31, 23, 59));
    election(ElectionStatus.OPEN, LocalDateTime.of(2026, 1, 1, 0, 0));
    election(ElectionStatus.OPEN, LocalDateTime.of(2027, 1, 1, 0, 0));
    electionDAO.insertElection(
        LEGACY_RP_TYPE,
        ElectionStatus.OPEN.getValue(),
        at(LocalDateTime.of(2026, 6, 1, 12, 0)),
        UUID.randomUUID().toString(),
        datasetId);

    assertEquals(
        List.of(new ElectionBucket(Q1, ElectionStatus.OPEN.getValue(), 1)),
        dao.countElectionsOpened(FROM, TO, null, "quarter"));
  }

  @Test
  void countsVotesCastByTypeWhenTheyWereCast() {
    Integer electionId = election(ElectionStatus.OPEN, LocalDateTime.of(2025, 12, 1, 12, 0));
    cast(electionId, VoteType.DAC, LocalDateTime.of(2026, 3, 31, 23, 0));
    cast(electionId, VoteType.DAC, LocalDateTime.of(2026, 4, 1, 1, 0));
    cast(electionId, VoteType.FINAL, LocalDateTime.of(2026, 4, 2, 1, 0));
    voteDAO.insertVote(userId, electionId, VoteType.DAC.getValue());

    assertEquals(
        List.of(
            new VoteBucket(Q1, VoteType.DAC.getValue(), 1),
            new VoteBucket(Q2, VoteType.DAC.getValue(), 1),
            new VoteBucket(Q2, VoteType.FINAL.getValue(), 1)),
        dao.countVotesCast(FROM, TO, null, "quarter"));
  }

  @Test
  void leavesOutUncastVotesEvenWhenUpdatedInTheRange() {
    LocalDateTime updated = LocalDateTime.of(2026, 6, 1, 12, 0);
    Integer electionId = election(ElectionStatus.OPEN, updated);
    voteDAO.insertVote(userId, electionId, VoteType.DAC.getValue());
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
    updateVote(null, "", at(updated), voteId, false, electionId, at(updated), false);

    assertTrue(dao.countVotesCast(FROM, TO, null, "quarter").isEmpty());
  }

  @Test
  void mergesStatusesStoredInAnyCase() {
    LocalDateTime created = LocalDateTime.of(2026, 6, 1, 12, 0);
    election(ElectionStatus.CANCELED, created);
    electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        "CANCELED",
        at(created),
        UUID.randomUUID().toString(),
        datasetId);

    assertEquals(
        List.of(new ElectionBucket(Q2, ElectionStatus.CANCELED.getValue(), 2)),
        dao.countElectionsOpened(FROM, TO, null, "quarter"));
  }

  @Test
  void datesACastVoteWithNoUpdateDateByItsCreateDateAndKeepsChairpersonSpelling() {
    Integer electionId = election(ElectionStatus.OPEN, LocalDateTime.of(2025, 12, 1, 12, 0));
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.CHAIRPERSON.getValue());
    updateVote(
        true, "", null, voteId, false, electionId, at(LocalDateTime.of(2026, 2, 1, 12, 0)), false);

    assertEquals(
        List.of(new VoteBucket(Q1, VoteType.CHAIRPERSON.getValue(), 1)),
        dao.countVotesCast(FROM, TO, null, "quarter"));
  }

  @Test
  void scopesElectionsAndVotesToTheDacsTheirDatasetsAreNowIn() {
    LocalDateTime opened = LocalDateTime.of(2026, 6, 1, 12, 0);
    Integer dacA = dac();
    Integer dacB = dac();
    Integer inA = election(ElectionStatus.OPEN, opened, dataset(dacA));
    Integer inB = election(ElectionStatus.OPEN, opened, dataset(dacB));
    election(ElectionStatus.OPEN, opened);
    cast(inA, VoteType.DAC, opened);
    cast(inB, VoteType.DAC, opened);
    cast(inB, VoteType.FINAL, opened);

    assertEquals(
        List.of(new ElectionBucket(Q2, ElectionStatus.OPEN.getValue(), 1)),
        dao.countElectionsOpened(FROM, TO, List.of(dacA), "quarter"));
    assertEquals(
        List.of(new VoteBucket(Q2, VoteType.DAC.getValue(), 1)),
        dao.countVotesCast(FROM, TO, List.of(dacA), "quarter"));
    assertEquals(
        List.of(new ElectionBucket(Q2, ElectionStatus.OPEN.getValue(), 2)),
        dao.countElectionsOpened(FROM, TO, List.of(dacA, dacB), "quarter"));
    assertEquals(
        List.of(
            new VoteBucket(Q2, VoteType.DAC.getValue(), 2),
            new VoteBucket(Q2, VoteType.FINAL.getValue(), 1)),
        dao.countVotesCast(FROM, TO, List.of(dacA, dacB), "quarter"));
    assertTrue(dao.countElectionsOpened(FROM, TO, List.of(), "quarter").isEmpty());
    assertTrue(dao.countVotesCast(FROM, TO, List.of(), "quarter").isEmpty());
  }

  private static Instant startOf(LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  private static Date at(LocalDateTime time) {
    return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
  }

  private Integer election(ElectionStatus status, LocalDateTime created) {
    return election(status, created, datasetId);
  }

  private Integer election(ElectionStatus status, LocalDateTime created, Integer onDataset) {
    return electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        status.getValue(),
        at(created),
        UUID.randomUUID().toString(),
        onDataset);
  }

  private Integer dataset(Integer dacId) {
    return datasetDAO.insertDataset(
        "Dataset " + UUID.randomUUID(),
        Timestamp.from(Instant.now()),
        userId,
        UUID.randomUUID().toString(),
        "{}",
        dacId);
  }

  private Integer dac() {
    return dacDAO.createDac("DAC " + UUID.randomUUID(), UUID.randomUUID().toString(), userId);
  }

  private void cast(Integer electionId, VoteType type, LocalDateTime castAt) {
    Integer voteId = voteDAO.insertVote(userId, electionId, type.getValue());
    updateVote(true, "", at(castAt), voteId, false, electionId, at(castAt), false);
  }
}
