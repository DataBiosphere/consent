package org.broadinstitute.consent.http.service.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.cloud.storage.BlobId;
import jakarta.ws.rs.core.MediaType;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.models.DaaAudit;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
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
    serviceDAO = new DaaServiceDAO(jdbi, daaDAO, fileStorageObjectDAO);
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
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), new Date());
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
          assertEquals(AuditActions.CREATE, audits.getFirst().action());
        });
  }

  @Test
  void testCreateDaa_nullFsoSuccess() throws Exception {
    User user = createUser();
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), new Date());
    Integer daaId = serviceDAO.createDaaWithFso(user.getUserId(), dacId, null);

    List<FileStorageObject> fsos = fileStorageObjectDAO.findFilesByEntityId(daaId.toString());
    assertTrue(fsos.isEmpty(), "Expected no FileStorageObjects to be created when FSO is null");
  }

  @Test
  void testCreateDaa_dacFKError() {
    User user = createUser();
    Integer dacId = 1; // Non-existent DAC ID to trigger daaDAO.createDaa error
    FileStorageObject fso = createFileStorageObject();
    assertThrows(
        UnableToExecuteStatementException.class,
        () -> serviceDAO.createDaaWithFso(user.getUserId(), dacId, fso));
    ILoggingEvent event = testAppender.getLoggedEvents().getFirst();
    assertThat(event.getFormattedMessage(), containsString("foreign key constraint"));
    assertThat(event.getFormattedMessage(), containsString("fk_daa_initial_dac_id"));
  }

  @Test
  void testCreateDaa_userFKError() {
    User user = new User();
    user.setUserId(1); // Non-existent user ID to trigger daaDAO.createDaa error
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), new Date());
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
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), new Date());
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
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), new Date());
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
            randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), new Date());
    FileStorageObject fso = createFileStorageObject();
    fso.setBlobId(null);
    assertThrows(
        IllegalArgumentException.class,
        () -> serviceDAO.createDaaWithFso(user.getUserId(), dacId, fso));
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
