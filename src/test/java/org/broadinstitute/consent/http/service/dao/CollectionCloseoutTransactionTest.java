package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.OntologyService;
import org.broadinstitute.consent.http.service.VoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CollectionCloseoutTransactionTest extends DAOTestHelper {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void voteAndRationaleWaitForCloseoutAndRejectAfterItCommits(boolean rationaleOnly)
      throws Exception {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    Integer electionId =
        createDataAccessElection(dar.getReferenceId(), createDatasetId()).getElectionId();
    Vote vote = createFinalVote(user.getUserId(), electionId);
    VoteService service =
        new VoteService(
            jdbi, new VoteServiceDAO(jdbi), mock(EmailService.class), mock(OntologyService.class));
    try (var executor = Executors.newSingleThreadExecutor();
        var handle = jdbi.open()) {
      handle.begin();
      DataAccessRequestDAO dao = handle.attach(DataAccessRequestDAO.class);
      dao.lockCollection(dar.getCollectionId());
      DataAccessRequestData closeout = new DataAccessRequestData();
      closeout.setCloseoutSupplement(
          new CloseoutSupplement(List.of("Completed"), "", user.getUserId()));
      dao.insertProgressReport(
          dar.getId(),
          dar.getCollectionId(),
          UUID.randomUUID().toString(),
          user.getUserId(),
          closeout,
          user.getEraCommonsId());
      CountDownLatch started = new CountDownLatch(1);
      Future<List<Vote>> waiting =
          executor.submit(
              () -> {
                started.countDown();
                return rationaleOnly
                    ? service.updateRationaleByVoteIds(
                        List.of(vote.getVoteId()), "synthetic rationale")
                    : service.updateVotesWithValue(List.of(vote), false, null, user);
              });
      try {
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> waiting.get(200, TimeUnit.MILLISECONDS));
      } finally {
        // Always release the database lock before joining the worker, including on failure.
        handle.commit();
      }
      ExecutionException failure =
          assertThrows(ExecutionException.class, () -> waiting.get(5, TimeUnit.SECONDS));
      assertInstanceOf(ConsentConflictException.class, failure.getCause());
      Vote unchanged = voteDAO.findVoteById(vote.getVoteId());
      assertNull(unchanged.getVote());
      assertNull(unchanged.getRationale());
    }
  }

  @Test
  void nestedVoteWriteDoesNotCommitTheOuterTransaction() {
    User user = createUser();
    DataAccessRequest dar = createDataAccessRequestV3();
    Integer electionId =
        createDataAccessElection(dar.getReferenceId(), createDatasetId()).getElectionId();
    Vote vote = createFinalVote(user.getUserId(), electionId);
    String originalStatus = electionDAO.findElectionById(electionId).getStatus();
    VoteServiceDAO voteServiceDAO = new VoteServiceDAO(jdbi);
    Integer collectionId = dar.getCollectionId();

    assertThrows(
        IllegalStateException.class,
        () -> writeVoteThenRollBack(collectionId, vote, voteServiceDAO));

    assertNull(voteDAO.findVoteById(vote.getVoteId()).getVote());
    assertEquals(originalStatus, electionDAO.findElectionById(electionId).getStatus());
  }

  /** Writes through VoteServiceDAO's own transaction nested inside an outer one, then aborts. */
  private void writeVoteThenRollBack(
      Integer collectionId, Vote vote, VoteServiceDAO voteServiceDAO) {
    dataAccessRequestDAO.inTransaction(
        dao -> {
          dao.lockCollection(collectionId);
          voteServiceDAO.updateVotesWithValue(List.of(vote), true, "synthetic rationale");
          throw new IllegalStateException("Force rollback");
        });
  }

  private Integer createDatasetId() {
    User user = createUser();
    return datasetDAO.insertDataset(
        "Synthetic dataset " + UUID.randomUUID(),
        FIXED_TIMESTAMP,
        user.getUserId(),
        UUID.randomUUID().toString(),
        EMPTY_JSON_DOCUMENT,
        null);
  }

  private Election createDataAccessElection(String referenceId, Integer datasetId) {
    Integer id =
        electionDAO.insertElection("DataAccess", "Open", FIXED_DATE, referenceId, datasetId);
    return electionDAO.findElectionById(id);
  }

  private Vote createFinalVote(Integer userId, Integer electionId) {
    Integer id = voteDAO.insertVote(userId, electionId, "final");
    return voteDAO.findVoteById(id);
  }
}
