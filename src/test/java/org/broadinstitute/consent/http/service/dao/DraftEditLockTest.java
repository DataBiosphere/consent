package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A draft edit checks draft status on an earlier read. This test runs the real edit while a submit
 * of the same DAR holds its row, so the edit's guarded update meets the submitted row.
 */
class DraftEditLockTest extends DAOTestHelper {

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
    // Interrupting the lock holder rolls it back, which frees the waiter; let both finish.
    assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
  }

  @Test
  void editWaitingOnASubmitDoesNotTurnTheSubmittedDarBackIntoADraft() throws Exception {
    User user = createUser();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(uniqueDarCode(), user.getUserId(), new Date());
    String referenceId = randomAlphanumeric(10);
    DataAccessRequestData original = new DataAccessRequestData();
    original.setOtherText("submitted content");
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), new Date(), null, new Date(), original, null);
    AtomicInteger holderPid = new AtomicInteger();
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    // The submit: sets submission_date and holds the row until it commits.
    Future<?> submit =
        executor.submit(
            () -> {
              jdbi.useTransaction(
                  h -> {
                    h.execute(
                        "UPDATE data_access_request SET submission_date = now() "
                            + "WHERE reference_id = ?",
                        referenceId);
                    holderPid.set(
                        h.createQuery("SELECT pg_backend_pid()").mapTo(Integer.class).one());
                    locked.countDown();
                    release.await(10, TimeUnit.SECONDS);
                  });
              return null;
            });
    assertTrue(locked.await(10, TimeUnit.SECONDS));

    // The edit read the DAR as a draft before the submit.
    DataAccessRequest edit = new DataAccessRequest();
    edit.setReferenceId(referenceId);
    edit.setCollectionId(collectionId);
    DataAccessRequestData changed = new DataAccessRequestData();
    changed.setOtherText("edited after submit");
    edit.setData(changed);
    edit.setDatasetIds(List.of());
    Future<DataAccessRequest> edited =
        executor.submit(() -> serviceDAO.updateByReferenceId(user, edit));
    awaitATransactionBlockedBy(holderPid.get());
    release.countDown();
    submit.get(10, TimeUnit.SECONDS);

    try {
      edited.get(10, TimeUnit.SECONDS);
      fail("The edit was saved over the submitted DAR");
    } catch (ExecutionException e) {
      assertInstanceOf(SubmittedDARCannotBeEditedException.class, e.getCause());
    }
    DataAccessRequest stored = dataAccessRequestDAO.findByReferenceId(referenceId);
    assertFalse(stored.getDraft());
    assertEquals("submitted content", stored.getData().getOtherText());
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
    fail("The edit never waited for the submit's row lock");
  }
}
