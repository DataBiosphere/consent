package org.broadinstitute.consent.http.service.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.DaaAudit;
import org.broadinstitute.consent.http.models.DaaBulkRelationResult;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.TestAppender;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@SuppressWarnings("java:S5778")
@ExtendWith(MockitoExtension.class)
class DaaServiceDAOTest extends DAOTestHelper {

  private static DaaServiceDAO serviceDAO;
  private TestAppender testAppender;

  @BeforeEach
  void setUp() {
    Logger testLogger = (Logger) LoggerFactory.getLogger(DaaServiceDAO.class);
    testLogger.setLevel(Level.TRACE);
    testAppender = new TestAppender();
    testAppender.reset();
    testLogger.addAppender(testAppender);
    testAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    testAppender.start();
    serviceDAO = new DaaServiceDAO(jdbi);
  }

  @AfterEach
  void tearDown() {
    testAppender.stop();
  }

  @Test
  void testCreateDaa() {
    User user = createUser();
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), user.getUserId());
    FileStorageObject fso = createFileStorageObject();
    assertDoesNotThrow(
        () -> {
          Integer daaId = serviceDAO.createDaaWithFso(user.getUserId(), dacId, fso);
          assertNotNull(daaId);
          DataAccessAgreement daa = daaDAO.findById(daaId);
          assertNotNull(daa);
          assertNotNull(daa.getFile());
          assertNotNull(daa.getInitialDacId());
          assertFalse(daa.getDacs().isEmpty());
          // Assert that audit record is created
          List<DaaAudit> audits = daaDAO.findAuditsByDaaId(daaId);
          assertNotNull(audits);
          assertFalse(audits.isEmpty());
          assertTrue(audits.stream().anyMatch(a -> a.action().equals(AuditActions.CREATE)));
          assertTrue(audits.stream().anyMatch(a -> a.action().equals(AuditActions.ADD)));
        });
  }

  @Test
  void testCreateDaa_nullFsoSuccess() throws Exception {
    User user = createUser();
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), user.getUserId());
    Integer daaId = serviceDAO.createDaaWithFso(user.getUserId(), dacId, null);

    List<FileStorageObject> fsos = fileStorageObjectDAO.findFilesByEntityId(daaId.toString());
    assertTrue(fsos.isEmpty(), "Expected no FileStorageObjects to be created when FSO is null");
    // Assert that CREATE audit records are created.
    List<DaaAudit> daaAudits = daaDAO.findAuditsByDaaId(daaId);
    assertNotNull(daaAudits);
    assertFalse(daaAudits.isEmpty());
    assertTrue(daaAudits.stream().anyMatch(a -> a.action().equals(AuditActions.CREATE)));
  }

  @Test
  void testCreateDaa_userFKError() {
    User user = new User();
    user.setUserId(1); // Non-existent user ID to trigger daaDAO.createDaa error
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(10),
            randomAlphabetic(10),
            randomAlphabetic(10),
            createUser().getUserId());
    FileStorageObject fso = createFileStorageObject();
    assertThrows(
        UnableToExecuteStatementException.class,
        () -> serviceDAO.createDaaWithFso(user.getUserId(), dacId, fso));
    ILoggingEvent event = testAppender.getLoggedEvents().getFirst();
    assertThat(event.getFormattedMessage(), containsString("foreign key constraint"));
    assertThat(event.getFormattedMessage(), containsString("fk_daa_create_user_id"));
  }

  @Test
  void testCreateDaa_nullFsoFileNameError() {
    User user = createUser();
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), user.getUserId());
    FileStorageObject fso = createFileStorageObject();
    fso.setFileName(null);
    assertThrows(
        IllegalArgumentException.class,
        () -> serviceDAO.createDaaWithFso(user.getUserId(), dacId, fso));
  }

  @Test
  void testCreateDaa_nullFsoCategoryError() {
    User user = createUser();
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), user.getUserId());
    FileStorageObject fso = createFileStorageObject();
    fso.setCategory(null);
    assertThrows(
        IllegalArgumentException.class,
        () -> serviceDAO.createDaaWithFso(user.getUserId(), dacId, fso));
  }

  @Test
  void testCreateDaa_nullFsoBlobIdError() {
    User user = createUser();
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), user.getUserId());
    FileStorageObject fso = createFileStorageObject();
    fso.setBlobId(null);
    assertThrows(
        IllegalArgumentException.class,
        () -> serviceDAO.createDaaWithFso(user.getUserId(), dacId, fso));
  }

  // ── Atomic bulk pre-authorization (DT-3325) ─────────────────────────────────────

  private static final int NONEXISTENT_DAA_ID = 999_999_999;

  private Integer createDaaId(User creator) {
    int dacId =
        dacDAO.createDac(
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), creator.getUserId());
    Instant now = Instant.now();
    return daaDAO.createDaa(creator.getUserId(), now, creator.getUserId(), now, dacId);
  }

  private LibraryCard insertCardFor(User user, User creator) {
    Integer id =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(),
            user.getDisplayName(),
            user.getEmail(),
            creator.getUserId(),
            new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }

  @Test
  void testBulkAddDaasToUserHappyPath() {
    User signingOfficial = createUser();
    User researcher = createUser();
    insertCardFor(researcher, signingOfficial);
    Integer daaId1 = createDaaId(signingOfficial);
    Integer daaId2 = createDaaId(signingOfficial);

    DaaBulkRelationResult result =
        serviceDAO.bulkAddDaasToUser(researcher, List.of(daaId1, daaId2), signingOfficial);

    assertEquals(2, result.getApplied());
    List<Integer> daaIds =
        libraryCardDAO.findLibraryCardByUserId(researcher.getUserId()).getDaaIds();
    assertTrue(daaIds.contains(daaId1));
    assertTrue(daaIds.contains(daaId2));
  }

  @Test
  void testBulkAddUsersToDaaAutoCreatesLibraryCard() {
    User signingOfficial = createUser();
    User researcher = createUser();
    // No library card inserted for the researcher — the transaction should create one.
    assertNull(libraryCardDAO.findLibraryCardByUserId(researcher.getUserId()));
    Integer daaId = createDaaId(signingOfficial);

    DaaBulkRelationResult result =
        serviceDAO.bulkAddUsersToDaa(daaId, List.of(researcher), signingOfficial);

    assertEquals(1, result.getApplied());
    LibraryCard card = libraryCardDAO.findLibraryCardByUserId(researcher.getUserId());
    assertNotNull(card);
    assertTrue(card.getDaaIds().contains(daaId));
  }

  @Test
  void testBulkAddDaasToUserRollsBackOnMidBatchFailure() {
    User signingOfficial = createUser();
    User researcher = createUser();
    insertCardFor(researcher, signingOfficial);
    Integer validDaaId = createDaaId(signingOfficial);

    // The second id violates the lc_daa -> data_access_agreement foreign key, failing mid-batch.
    assertThrows(
        Exception.class,
        () ->
            serviceDAO.bulkAddDaasToUser(
                researcher, List.of(validDaaId, NONEXISTENT_DAA_ID), signingOfficial));

    // The valid relation inserted before the failure must have been rolled back.
    List<Integer> daaIds =
        libraryCardDAO.findLibraryCardByUserId(researcher.getUserId()).getDaaIds();
    assertFalse(daaIds.contains(validDaaId));
  }

  @Test
  void testBulkRemoveDaasFromUser() {
    User signingOfficial = createUser();
    User researcher = createUser();
    LibraryCard card = insertCardFor(researcher, signingOfficial);
    Integer daaId = createDaaId(signingOfficial);
    libraryCardDAO.createLibraryCardDaaRelation(
        researcher.getUserId(), signingOfficial.getUserId(), card.getId(), daaId);
    assertTrue(
        libraryCardDAO.findLibraryCardByUserId(researcher.getUserId()).getDaaIds().contains(daaId));

    DaaBulkRelationResult result =
        serviceDAO.bulkRemoveDaasFromUser(researcher, List.of(daaId), signingOfficial);

    assertEquals(1, result.getApplied());
    assertFalse(
        libraryCardDAO.findLibraryCardByUserId(researcher.getUserId()).getDaaIds().contains(daaId));
  }

  @Test
  void testBulkRemoveUsersFromDaa() {
    User signingOfficial = createUser();
    User researcher = createUser();
    LibraryCard card = insertCardFor(researcher, signingOfficial);
    Integer daaId = createDaaId(signingOfficial);
    libraryCardDAO.createLibraryCardDaaRelation(
        researcher.getUserId(), signingOfficial.getUserId(), card.getId(), daaId);

    DaaBulkRelationResult result =
        serviceDAO.bulkRemoveUsersFromDaa(daaId, List.of(researcher), signingOfficial);

    assertEquals(1, result.getApplied());
    assertFalse(
        libraryCardDAO.findLibraryCardByUserId(researcher.getUserId()).getDaaIds().contains(daaId));
  }

  @Test
  void testBulkAddDaasToUserCountsOnlyGenuinelyNewRelations() {
    User signingOfficial = createUser();
    User researcher = createUser();
    LibraryCard card = insertCardFor(researcher, signingOfficial);
    Integer alreadyLinkedDaaId = createDaaId(signingOfficial);
    Integer newDaaId = createDaaId(signingOfficial);
    // Pre-link the first DAA so re-adding it is a no-op (ON CONFLICT DO NOTHING).
    libraryCardDAO.createLibraryCardDaaRelation(
        researcher.getUserId(), signingOfficial.getUserId(), card.getId(), alreadyLinkedDaaId);

    DaaBulkRelationResult result =
        serviceDAO.bulkAddDaasToUser(
            researcher, List.of(alreadyLinkedDaaId, newDaaId), signingOfficial);

    // applied must reflect only the one relation that actually changed, not the requested count.
    assertEquals(2, result.getRequested());
    assertEquals(1, result.getApplied());
    assertEquals(1, result.getSkipped());
  }

  @Test
  void testBulkRemoveDaasFromUserCountsOnlyRelationsThatExisted() {
    User signingOfficial = createUser();
    User researcher = createUser();
    insertCardFor(researcher, signingOfficial);
    Integer neverLinkedDaaId = createDaaId(signingOfficial);
    // No relation was ever created for this DAA, so removing it is a no-op.

    DaaBulkRelationResult result =
        serviceDAO.bulkRemoveDaasFromUser(researcher, List.of(neverLinkedDaaId), signingOfficial);

    assertEquals(1, result.getRequested());
    assertEquals(0, result.getApplied());
    assertEquals(1, result.getSkipped());
  }

  private FileStorageObject createFileStorageObject() {
    FileStorageObject fso = new FileStorageObject();
    fso.setFileName(randomAlphabetic(10));
    fso.setCategory(FileCategory.DATA_ACCESS_AGREEMENT);
    BlobId blobId = BlobId.of(randomAlphabetic(10), randomAlphabetic(10));
    fso.setBlobId(blobId);
    fso.setMediaType(MediaType.TEXT_PLAIN_TYPE.getType());
    return fso;
  }
}
