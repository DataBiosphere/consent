package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Resubmit checks for elections under the collection lock that every election-opening path also
 * takes. This test runs the real resubmit while another transaction opens an election.
 */
class ResubmitElectionLockTest extends DAOTestHelper {

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
  void resubmitWaitingOnAnElectionBeingOpenedIsRejected() throws Exception {
    DataAccessRequest dar = createDataAccessRequestV3();
    Integer collectionId = dar.getCollectionId();
    // What the researcher's resubmit loaded: no elections yet.
    DarCollection loaded = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    AtomicInteger holderPid = new AtomicInteger();
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    // A chair opening an election: holds the collection lock until it commits.
    Future<?> opening =
        executor.submit(
            () -> {
              jdbi.useTransaction(
                  h -> {
                    h.attach(DataAccessRequestDAO.class).lockCollection(collectionId);
                    h.attach(ElectionDAO.class)
                        .insertElection(
                            ElectionType.DATA_ACCESS.getValue(),
                            ElectionStatus.OPEN.getValue(),
                            dar.getReferenceId(),
                            null);
                    holderPid.set(
                        h.createQuery("SELECT pg_backend_pid()").mapTo(Integer.class).one());
                    locked.countDown();
                    release.await(10, TimeUnit.SECONDS);
                  });
              return null;
            });
    assertTrue(locked.await(10, TimeUnit.SECONDS));

    Future<?> resubmit = executor.submit(() -> service.updateCollectionToDraftStatus(loaded));
    awaitATransactionBlockedBy(holderPid.get());
    release.countDown();
    opening.get(10, TimeUnit.SECONDS);

    try {
      resubmit.get(10, TimeUnit.SECONDS);
      fail("The collection was turned back into drafts after an election was opened");
    } catch (ExecutionException e) {
      assertInstanceOf(ConsentConflictException.class, e.getCause());
    }
    assertFalse(dataAccessRequestDAO.findByReferenceId(dar.getReferenceId()).getDraft());
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
    fail("The resubmit never waited for the collection lock");
  }
}
