package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A signing official's approval is for the submission they loaded. These tests run the real
 * approval against the database while another transaction holds the collection lock.
 */
class SigningOfficialApprovalLockTest extends DAOTestHelper {

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
  void approvalOfASubmissionReplacedWhileWaitingIsRejected() throws Exception {
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId());
    DataAccessRequest dar = createDarAwaitingApprovalBy(signingOfficial);
    Integer collectionId = dar.getCollectionId();
    // What the signing official's page loaded.
    DarCollection loaded = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    AtomicInteger holderPid = new AtomicInteger();
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    // Another transaction changes the DAR's submission date while holding the collection lock.
    Future<?> resubmitted =
        executor.submit(
            () -> {
              jdbi.useTransaction(
                  h -> {
                    h.attach(DataAccessRequestDAO.class).lockCollection(collectionId);
                    h.execute(
                        "UPDATE data_access_request SET submission_date = ? WHERE reference_id = ?",
                        new Timestamp(dar.getSubmissionDate().getTime() + 60_000),
                        dar.getReferenceId());
                    holderPid.set(
                        h.createQuery("SELECT pg_backend_pid()").mapTo(Integer.class).one());
                    locked.countDown();
                    release.await(10, TimeUnit.SECONDS);
                  });
              return null;
            });
    assertTrue(locked.await(10, TimeUnit.SECONDS));

    Future<?> approval =
        executor.submit(
            () ->
                service.approveDarCollection(
                    signingOfficial, loaded, mock(ContainerRequest.class), null));
    awaitATransactionBlockedBy(holderPid.get());
    release.countDown();
    resubmitted.get(10, TimeUnit.SECONDS);

    try {
      approval.get(10, TimeUnit.SECONDS);
      fail("The approval of the replaced submission was recorded");
    } catch (ExecutionException e) {
      assertInstanceOf(ConsentConflictException.class, e.getCause());
    }
    assertNull(
        dataAccessRequestDAO
            .findByReferenceId(dar.getReferenceId())
            .getApprovingSigningOfficialUserId());
  }

  @Test
  void approvalOfAnUnchangedSubmissionIsRecorded() {
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId());
    DataAccessRequest dar = createDarAwaitingApprovalBy(signingOfficial);
    DarCollection loaded = darCollectionDAO.findDARCollectionByCollectionId(dar.getCollectionId());

    service.approveDarCollection(signingOfficial, loaded, mock(ContainerRequest.class), null);

    // The submission date read under the lock matches the one loaded (to the millisecond).
    assertEquals(
        signingOfficial.getUserId(),
        dataAccessRequestDAO
            .findByReferenceId(dar.getReferenceId())
            .getApprovingSigningOfficialUserId());
  }

  /** A submitted DAR that needs this signing official's approval, alone in its collection. */
  private DataAccessRequest createDarAwaitingApprovalBy(User signingOfficial) {
    DataAccessRequest dar = createDataAccessRequestV3();
    DataAccessRequestData data = dar.getData();
    data.setSigningOfficialEmail(signingOfficial.getEmail());
    dataAccessRequestDAO.updateDataByReferenceId(
        dar.getReferenceId(), dar.getUserId(), data, dar.getEraCommonsId());
    dataAccessRequestDAO.updateRequiresSOApproval(true, dar.getReferenceId());
    // Leave only this DAR in the collection, so it is the one the approval acts on.
    jdbi.useHandle(
        h ->
            h.execute(
                "DELETE FROM data_access_request WHERE collection_id = ? AND reference_id <> ?",
                dar.getCollectionId(),
                dar.getReferenceId()));
    return dar;
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
    fail("The approval never waited for the lock");
  }
}
