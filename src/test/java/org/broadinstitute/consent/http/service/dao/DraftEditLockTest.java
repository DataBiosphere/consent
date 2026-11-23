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
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.enumeration.DarDocumentType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
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
        collectionId, referenceId, user.getUserId(), original, null);
    setDarSubmissionDate(referenceId, null);
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

  @Test
  void editWaitingOnAnElectionBeingOpenedIsRejected() throws Exception {
    User user = createUser();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(uniqueDarCode(), user.getUserId(), new Date());
    String referenceId = randomAlphanumeric(10);
    DataAccessRequestData original = new DataAccessRequestData();
    original.setOtherText("reviewed content");
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), original, null);
    setDarSubmissionDate(referenceId, null);
    AtomicInteger holderPid = new AtomicInteger();
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    // Another transaction holds the collection lock and opens an election on the DAR.
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
                            referenceId,
                            null);
                    holderPid.set(
                        h.createQuery("SELECT pg_backend_pid()").mapTo(Integer.class).one());
                    locked.countDown();
                    release.await(10, TimeUnit.SECONDS);
                  });
              return null;
            });
    assertTrue(locked.await(10, TimeUnit.SECONDS));

    DataAccessRequest edit = new DataAccessRequest();
    edit.setReferenceId(referenceId);
    edit.setCollectionId(collectionId);
    DataAccessRequestData changed = new DataAccessRequestData();
    changed.setOtherText("edited");
    edit.setData(changed);
    edit.setDatasetIds(List.of());
    Future<DataAccessRequest> edited =
        executor.submit(() -> serviceDAO.updateByReferenceId(user, edit));
    awaitATransactionBlockedBy(holderPid.get());
    release.countDown();
    opening.get(10, TimeUnit.SECONDS);

    try {
      edited.get(10, TimeUnit.SECONDS);
      fail("The edit was saved after an election was opened");
    } catch (ExecutionException e) {
      assertInstanceOf(ConsentConflictException.class, e.getCause());
    }
    assertEquals(
        "reviewed content",
        dataAccessRequestDAO.findByReferenceId(referenceId).getData().getOtherText());
  }

  // An upload reads the location it replaces under the row lock, not from its earlier read.
  @Test
  void uploadWaitingOnAnotherUploadReplacesTheLocationThatOneSaved() throws Exception {
    User user = createUser();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(uniqueDarCode(), user.getUserId(), new Date());
    String referenceId = randomAlphanumeric(10);
    DataAccessRequestData original = new DataAccessRequestData();
    original.setIrbDocumentLocation("original-irb");
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), original, null);
    setDarSubmissionDate(referenceId, null);
    // The second upload read the draft while it still pointed to the original document.
    DataAccessRequest second = dataAccessRequestDAO.findByReferenceId(referenceId);
    second.getData().setIrbDocumentLocation("second-irb");
    second.getData().setIrbDocumentName("second-irb.pdf");
    AtomicInteger holderPid = new AtomicInteger();
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    // The first upload saves its document and holds the row until it commits.
    Future<?> first =
        executor.submit(
            () -> {
              jdbi.useTransaction(
                  h -> {
                    h.execute(
                        "UPDATE data_access_request SET data = data || "
                            + "'{\"irbDocumentLocation\": \"first-irb\"}'::jsonb "
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

    Future<DataAccessRequestServiceDAO.DocumentSave> saved =
        executor.submit(() -> serviceDAO.saveDocument(user, second, DarDocumentType.IRB));
    awaitATransactionBlockedBy(holderPid.get());
    release.countDown();
    first.get(10, TimeUnit.SECONDS);

    // The first upload's document is the one the draft no longer refers to.
    assertEquals("first-irb", saved.get(10, TimeUnit.SECONDS).previousLocation());
    assertEquals(
        "second-irb",
        dataAccessRequestDAO.findByReferenceId(referenceId).getData().getIrbDocumentLocation());
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
