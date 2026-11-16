package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Opening elections re-checks, under the collection lock, that the DAR is still submitted with the
 * date the chair loaded. This test runs the chair's real election path while another transaction
 * turns the collection's DARs back into drafts.
 */
class ElectionDraftLockTest extends DAOTestHelper {

  private ExecutorService executor;
  private DarCollectionService service;

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(2);
    service =
        new DarCollectionService(
            jdbi,
            new DarCollectionServiceDAO(jdbi),
            mock(EmailService.class),
            mock(DACAutomationRuleService.class),
            mock(OntologyService.class));
  }

  @AfterEach
  void stopExecutor() throws InterruptedException {
    executor.shutdownNow();
    // Interrupting the lock holder rolls it back, which frees the waiter; let both finish.
    assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
  }

  @Test
  void electionsOnACollectionResubmittedWhileWaitingAreRejected() throws Exception {
    Integer dacId =
        dacDAO.createDac(randomAlphabetic(10), randomAlphabetic(10), createUser().getUserId());
    Integer chairId = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dacId).getUserId();
    User chair = userDAO.findUserById(chairId);
    Integer datasetId =
        datasetDAO.insertDataset(
            randomAlphabetic(10),
            new Timestamp(System.currentTimeMillis()),
            chairId,
            null,
            null,
            dacId);
    DataAccessRequest dar = createDataAccessRequestV3();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), datasetId);
    Integer collectionId = dar.getCollectionId();
    // What the chair's page loaded: every DAR submitted.
    DarCollection loaded = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    AtomicInteger holderPid = new AtomicInteger();
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    // Another transaction holds the collection lock and turns the DARs back into drafts.
    Future<?> resubmit =
        executor.submit(
            () -> {
              jdbi.useTransaction(
                  h -> {
                    h.attach(DataAccessRequestDAO.class).lockCollection(collectionId);
                    h.execute(
                        "UPDATE data_access_request SET submission_date = NULL "
                            + "WHERE collection_id = ?",
                        collectionId);
                    holderPid.set(
                        h.createQuery("SELECT pg_backend_pid()").mapTo(Integer.class).one());
                    locked.countDown();
                    release.await(10, TimeUnit.SECONDS);
                  });
              return null;
            });
    assertTrue(locked.await(10, TimeUnit.SECONDS));

    Future<?> elections =
        executor.submit(() -> service.createElectionsForDarCollection(chair, loaded));
    awaitATransactionBlockedBy(holderPid.get());
    release.countDown();
    resubmit.get(10, TimeUnit.SECONDS);

    try {
      elections.get(10, TimeUnit.SECONDS);
      fail("Elections were opened on a draft");
    } catch (ExecutionException e) {
      assertInstanceOf(ConsentConflictException.class, e.getCause());
    }
    assertTrue(electionDAO.findElectionsByReferenceId(dar.getReferenceId()).isEmpty());
  }

  /** Waits until a transaction is blocked by the given backend, so the test exercises the wait. */
  private static void awaitATransactionBlockedBy(int holderPid) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline) {
      boolean blocked =
          jdbi.withHandle(
              h ->
                  h.createQuery(
                          "SELECT EXISTS (SELECT 1 FROM pg_stat_activity "
                              + "WHERE :holder = ANY(pg_blocking_pids(pid)))")
                      .bind("holder", holderPid)
                      .mapTo(Boolean.class)
                      .one());
      if (blocked) {
        return;
      }
      Thread.sleep(20);
    }
    fail("The chair's request never waited for the collection lock");
  }
}
