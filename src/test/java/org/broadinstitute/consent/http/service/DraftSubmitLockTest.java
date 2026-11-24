package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Submitting a draft re-checks it after locking its DAR row. These tests run that check in a real
 * second transaction while another transaction holds the row.
 */
class DraftSubmitLockTest extends DAOTestHelper {

  private ExecutorService executor;
  private DataAccessRequestServiceDAO serviceDAO;

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(2);
    serviceDAO = new DataAccessRequestServiceDAO(jdbi);
  }

  @AfterEach
  void stopExecutor() throws InterruptedException {
    executor.shutdownNow();
    // Interrupting the lock holder ends its wait and it commits, which frees the waiter; let both
    // finish.
    assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
  }

  @Test
  void secondSubmitOfTheSameDraftIsRejected() throws Exception {
    Draft draft = createDraftInACollection();
    AtomicInteger holderPid = new AtomicInteger();
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    // The first submit: locks the DAR row, submits it, and commits when released.
    Future<?> first =
        executor.submit(
            () ->
                serviceDAO.inTransaction(
                    tx -> {
                      tx.dataAccessRequestDAO().lockDataAccessRequest(draft.referenceId());
                      tx.dataAccessRequestDAO()
                          .updateDraftToSubmittedForCollection(
                              draft.collectionId(), draft.referenceId());
                      holderPid.set(
                          tx.dataAccessRequestDAO()
                              .getHandle()
                              .createQuery("SELECT pg_backend_pid()")
                              .mapTo(Integer.class)
                              .one());
                      locked.countDown();
                      awaitQuietly(release);
                      return null;
                    }));
    assertTrue(locked.await(10, TimeUnit.SECONDS));

    // The second submit read the DAR as a draft before the first one committed.
    Future<?> second =
        executor.submit(
            () ->
                serviceDAO.inTransaction(
                    tx ->
                        DataAccessRequestService.validateDraftUnderLock(tx, draft.referenceId())));
    awaitATransactionBlockedBy(holderPid.get());
    release.countDown();
    first.get(10, TimeUnit.SECONDS);

    try {
      second.get(10, TimeUnit.SECONDS);
      fail("The second submit passed the draft check");
    } catch (ExecutionException e) {
      assertInstanceOf(SubmittedDARCannotBeEditedException.class, e.getCause());
    }
  }

  private record Draft(Integer collectionId, String referenceId) {}

  private Draft createDraftInACollection() {
    User user = createUser();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(uniqueDarCode(), user.getUserId(), new Date());
    String referenceId = randomAlphanumeric(10);
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), new DataAccessRequestData(), null);
    setDarSubmissionDate(referenceId, null);
    return new Draft(collectionId, referenceId);
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
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
    fail("The second submit never waited for the DAR row lock");
  }
}
