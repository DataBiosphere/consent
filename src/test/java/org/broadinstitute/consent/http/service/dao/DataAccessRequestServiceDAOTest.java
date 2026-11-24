package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.DarDocumentDeletionReason;
import org.broadinstitute.consent.http.enumeration.DarDocumentType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.exceptions.SubmittedDARCannotBeEditedException;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.CounterService;
import org.broadinstitute.consent.http.service.dao.DataAccessRequestServiceDAO.RemovedDocument;
import org.jdbi.v3.core.JdbiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestServiceDAOTest extends DAOTestHelper {

  DataAccessRequestServiceDAO serviceDAO;

  @BeforeEach
  void initService() {
    serviceDAO = new DataAccessRequestServiceDAO(jdbi);
  }

  @Test
  void testUpdateByReferenceId() {

    Dataset datasetOne = createDataset();
    Dataset datasetTwo = createDataset();
    Dataset datasetThree = createDataset();
    User user = createUser();
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2020);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    Date old = cal.getTime();

    String referenceId = randomAlphanumeric(10);
    DarDataset oldDarDataset = new DarDataset(referenceId, datasetOne.getDatasetId());
    DarDataset oldDarDatasetTwo = new DarDataset(referenceId, datasetTwo.getDatasetId());
    DarCollection collection = createDarCollection();
    Integer collectionId = collection.getDarCollectionId();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        new DataAccessRequestData(),
        user.getEraCommonsId());
    setDarDates(referenceId, old, null, old);
    dataAccessRequestDAO.insertAllDarDatasets(List.of(oldDarDataset, oldDarDatasetTwo));

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    dar.setCollectionId(collectionId);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setOtherText("This is a test value");
    List<Integer> newDatasetIds = List.of(datasetThree.getDatasetId());
    dar.setDatasetIds(newDatasetIds);
    dar.setData(data);

    initService();

    DataAccessRequest updatedDar = serviceDAO.updateByReferenceId(user, dar);

    Timestamp oldTimestamp = new Timestamp(old.getTime());
    assertFalse(oldTimestamp.equals(updatedDar.getUpdateDate()));
    assertEquals(newDatasetIds, updatedDar.getDatasetIds());
    DataAccessRequestData updatedData = updatedDar.getData();
    assertEquals(data.getOtherText(), updatedData.getOtherText());

    DarCollection targetCollection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    assertEquals(user.getUserId(), targetCollection.getUpdateUserId());

    // Within the last minute, not near the epoch.
    long age = System.currentTimeMillis() - targetCollection.getUpdateDate().getTime();
    assertTrue(age >= 0 && age < 60_000, "collection update date is " + age + " ms old");
  }

  @Test
  void testUpdateByReferenceId_NullCollectionId() {
    Dataset dataset = createDataset();
    User user = createUser();
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2020);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    Date old = cal.getTime();

    String referenceId = randomAlphanumeric(10);
    DarCollection collection = createDarCollection();
    Integer collectionId = collection.getDarCollectionId();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        new DataAccessRequestData(),
        user.getEraCommonsId());
    setDarDates(referenceId, old, null, old);

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    // collectionId intentionally not set — getCollectionId() returns null, skipping collection
    // update
    DataAccessRequestData data = new DataAccessRequestData();
    data.setOtherText("test");
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    dar.setData(data);

    DataAccessRequest updatedDar = serviceDAO.updateByReferenceId(user, dar);

    Timestamp oldTimestamp = new Timestamp(old.getTime());
    assertFalse(oldTimestamp.equals(updatedDar.getUpdateDate()));
    assertEquals(List.of(dataset.getDatasetId()), updatedDar.getDatasetIds());
  }

  @Test
  void testUpdateByReferenceIdRollsBackOnAJavaException() {
    Dataset dataset = createDataset();
    User user = createUser();
    String referenceId = randomAlphanumeric(10);
    DataAccessRequestData original = new DataAccessRequestData();
    original.setOtherText("original");
    dataAccessRequestDAO.insertDataAccessRequest(
        createDarCollection().getDarCollectionId(),
        referenceId,
        user.getUserId(),
        original,
        user.getEraCommonsId());
    setDarSubmissionDate(referenceId, null);
    dataAccessRequestDAO.insertAllDarDatasets(
        List.of(new DarDataset(referenceId, dataset.getDatasetId())));
    DataAccessRequest dar = spy(new DataAccessRequest());
    dar.setReferenceId(referenceId);
    DataAccessRequestData changed = new DataAccessRequestData();
    changed.setOtherText("changed");
    dar.setData(changed);
    // Read only after the update and the relation delete have run.
    doThrow(new IllegalStateException("dataset ids")).when(dar).getDatasetIds();

    assertThrows(IllegalStateException.class, () -> serviceDAO.updateByReferenceId(user, dar));

    DataAccessRequest stored = dataAccessRequestDAO.findByReferenceId(referenceId);
    assertEquals("original", stored.getData().getOtherText());
    assertEquals(List.of(dataset.getDatasetId()), stored.getDatasetIds());
  }

  @Test
  void testUpdateByReferenceIdRejectsSubmittedDar() {
    Dataset dataset = createDataset();
    Dataset otherDataset = createDataset();
    User user = createUser();
    String referenceId = randomAlphanumeric(10);
    DataAccessRequestData original = new DataAccessRequestData();
    original.setOtherText("original");
    Integer collectionId = createDarCollection().getDarCollectionId();
    // Submitted after the caller read it as a draft.
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), original, user.getEraCommonsId());
    dataAccessRequestDAO.insertAllDarDatasets(
        List.of(new DarDataset(referenceId, dataset.getDatasetId())));
    // Submitted, with an election: still reported as submitted (422), not 409.
    electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        referenceId,
        dataset.getDatasetId());
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    dar.setCollectionId(collectionId);
    DataAccessRequestData changed = new DataAccessRequestData();
    changed.setOtherText("changed");
    dar.setData(changed);
    dar.setDatasetIds(List.of(otherDataset.getDatasetId()));

    assertThrows(
        SubmittedDARCannotBeEditedException.class, () -> serviceDAO.updateByReferenceId(user, dar));

    DataAccessRequest stored = dataAccessRequestDAO.findByReferenceId(referenceId);
    assertFalse(stored.getDraft());
    assertEquals("original", stored.getData().getOtherText());
    assertEquals(List.of(dataset.getDatasetId()), stored.getDatasetIds());
  }

  @Test
  void testUpdateByReferenceIdRejectsDraftWithElections() {
    Dataset dataset = createDataset();
    Dataset otherDataset = createDataset();
    User user = createUser();
    String referenceId = randomAlphanumeric(10);
    DataAccessRequestData original = new DataAccessRequestData();
    original.setOtherText("original");
    // A draft that has an election.
    dataAccessRequestDAO.insertDataAccessRequest(
        createDarCollection().getDarCollectionId(),
        referenceId,
        user.getUserId(),
        original,
        user.getEraCommonsId());
    setDarSubmissionDate(referenceId, null);
    dataAccessRequestDAO.insertAllDarDatasets(
        List.of(new DarDataset(referenceId, dataset.getDatasetId())));
    electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CLOSED.getValue(),
        referenceId,
        dataset.getDatasetId());
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(referenceId);
    DataAccessRequestData changed = new DataAccessRequestData();
    changed.setOtherText("changed");
    dar.setData(changed);
    dar.setDatasetIds(List.of(otherDataset.getDatasetId()));

    assertThrows(ConsentConflictException.class, () -> serviceDAO.updateByReferenceId(user, dar));

    DataAccessRequest stored = dataAccessRequestDAO.findByReferenceId(referenceId);
    assertEquals("original", stored.getData().getOtherText());
    assertEquals(List.of(dataset.getDatasetId()), stored.getDatasetIds());
  }

  @Test
  void testUpdateByReferenceIdOfADeletedDraftIsNotFound() {
    User user = createUser();
    DataAccessRequest dar = new DataAccessRequest();
    // Deleted after the caller read it as a draft.
    dar.setReferenceId(randomAlphanumeric(10));
    dar.setData(new DataAccessRequestData());
    dar.setDatasetIds(List.of());

    assertThrows(NotFoundException.class, () -> serviceDAO.updateByReferenceId(user, dar));
  }

  @Test
  void testUpdateByReferenceIdKeepsTheRowsStatus() {
    User user = createUser();
    // A status the server set; findByReferenceId hides archived DARs, so not "Archived".
    String archived = randomAlphanumeric(10);
    DataAccessRequestData archivedData = new DataAccessRequestData();
    archivedData.setStatus("ServerValue");
    String noStatus = randomAlphanumeric(10);
    for (String referenceId : List.of(archived, noStatus)) {
      dataAccessRequestDAO.insertDataAccessRequest(
          createDarCollection().getDarCollectionId(),
          referenceId,
          user.getUserId(),
          referenceId.equals(archived) ? archivedData : new DataAccessRequestData(),
          user.getEraCommonsId());
      setDarSubmissionDate(referenceId, null);
    }

    for (String referenceId : List.of(archived, noStatus)) {
      DataAccessRequest edit = new DataAccessRequest();
      edit.setReferenceId(referenceId);
      DataAccessRequestData data = new DataAccessRequestData();
      data.setStatus("ClientValue");
      data.setOtherText("edited");
      edit.setData(data);
      edit.setDatasetIds(List.of());
      serviceDAO.updateByReferenceId(user, edit);
    }

    DataAccessRequest storedArchived = dataAccessRequestDAO.findByReferenceId(archived);
    assertEquals("ServerValue", storedArchived.getData().getStatus());
    assertEquals("edited", storedArchived.getData().getOtherText());
    assertNull(dataAccessRequestDAO.findByReferenceId(noStatus).getData().getStatus());
  }

  private String insertDraftWithIrbDocument(User user, String location) {
    String referenceId = randomAlphanumeric(10);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setIrbDocumentLocation(location);
    data.setIrbDocumentName(location + ".pdf");
    data.setOtherText("original");
    dataAccessRequestDAO.insertDataAccessRequest(
        createDarCollection().getDarCollectionId(),
        referenceId,
        user.getUserId(),
        data,
        user.getEraCommonsId());
    setDarSubmissionDate(referenceId, null);
    return referenceId;
  }

  @Test
  void testSaveDocumentKeepsAnEditThatCommittedMeanwhile() {
    User user = createUser();
    String referenceId = insertDraftWithIrbDocument(user, "own-irb");
    // The upload read the draft, then an edit committed before the upload saved.
    DataAccessRequest upload = dataAccessRequestDAO.findByReferenceId(referenceId);
    DataAccessRequest edit = dataAccessRequestDAO.findByReferenceId(referenceId);
    edit.getData().setOtherText("edited");
    edit.setDatasetIds(List.of());
    serviceDAO.updateByReferenceId(user, edit);
    upload.getData().setIrbDocumentLocation("new-irb");
    upload.getData().setIrbDocumentName("new-irb.pdf");

    var saved = serviceDAO.saveDocument(user, upload, DarDocumentType.IRB);

    assertEquals("own-irb", saved.previousLocation());
    DataAccessRequestData stored = dataAccessRequestDAO.findByReferenceId(referenceId).getData();
    assertEquals("new-irb", stored.getIrbDocumentLocation());
    assertEquals("new-irb.pdf", stored.getIrbDocumentName());
    assertEquals("edited", stored.getOtherText());
  }

  @Test
  void testSaveDocumentWithoutAPreviousDocument() {
    User user = createUser();
    String referenceId = insertDraftWithIrbDocument(user, "own-irb");
    DataAccessRequest upload = dataAccessRequestDAO.findByReferenceId(referenceId);
    upload.getData().setCollaborationLetterLocation("new-letter");
    upload.getData().setCollaborationLetterName("new-letter.pdf");

    var saved = serviceDAO.saveDocument(user, upload, DarDocumentType.COLLABORATION);

    assertNull(saved.previousLocation());
    DataAccessRequestData stored = dataAccessRequestDAO.findByReferenceId(referenceId).getData();
    assertEquals("new-letter", stored.getCollaborationLetterLocation());
    assertEquals("own-irb", stored.getIrbDocumentLocation());
  }

  @Test
  void testSaveDocumentRejectsASubmittedOrMissingDar() {
    User user = createUser();
    String referenceId = insertDraftWithIrbDocument(user, "own-irb");
    DataAccessRequest upload = dataAccessRequestDAO.findByReferenceId(referenceId);
    upload.getData().setIrbDocumentLocation("new-irb");
    dataAccessRequestDAO.updateDraftToSubmittedForCollection(upload.getCollectionId(), referenceId);

    assertThrows(
        SubmittedDARCannotBeEditedException.class,
        () -> serviceDAO.saveDocument(user, upload, DarDocumentType.IRB));
    assertEquals(
        "own-irb",
        dataAccessRequestDAO.findByReferenceId(referenceId).getData().getIrbDocumentLocation());
    upload.setReferenceId(randomAlphanumeric(10));
    assertThrows(
        NotFoundException.class, () -> serviceDAO.saveDocument(user, upload, DarDocumentType.IRB));
  }

  @Test
  void testUpdateByReferenceIdKeepsTheRowsDocuments() {
    User user = createUser();
    String referenceId = randomAlphanumeric(10);
    DataAccessRequestData stored = new DataAccessRequestData();
    stored.setIrbDocumentLocation("own-irb");
    stored.setIrbDocumentName("own-irb.pdf");
    stored.setCollaborationLetterLocation("own-letter");
    stored.setCollaborationLetterName("own-letter.pdf");
    dataAccessRequestDAO.insertDataAccessRequest(
        createDarCollection().getDarCollectionId(),
        referenceId,
        user.getUserId(),
        stored,
        user.getEraCommonsId());
    setDarSubmissionDate(referenceId, null);

    // The IRB document is kept from the row whatever the edit sends; the letter was removed.
    DataAccessRequest edit = new DataAccessRequest();
    edit.setReferenceId(referenceId);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setIrbDocumentLocation("other-irb");
    data.setOtherText("edited");
    edit.setData(data);
    edit.setDatasetIds(List.of());
    var result =
        serviceDAO.updateByReferenceId(
            user, edit, List.of("status", "irbDocumentLocation", "irbDocumentName"));

    // The removed letter is reported for deletion; the kept IRB document isn't.
    assertEquals(
        List.of(new RemovedDocument(DarDocumentType.COLLABORATION, "own-letter")),
        result.removedDocuments());
    DataAccessRequestData saved = dataAccessRequestDAO.findByReferenceId(referenceId).getData();
    assertEquals("own-irb", saved.getIrbDocumentLocation());
    assertEquals("own-irb.pdf", saved.getIrbDocumentName());
    assertNull(saved.getCollaborationLetterLocation());
    assertNull(saved.getCollaborationLetterName());
    assertEquals("edited", saved.getOtherText());
  }

  // The edit reports every location it removed; whether another DAR still uses it is checked
  // after the commit, before the file is deleted.
  @Test
  void testUpdateByReferenceIdReportsARemovedLocationAnotherDarUses() {
    User user = createUser();
    DataAccessRequestData shared = new DataAccessRequestData();
    shared.setCollaborationLetterLocation("shared-letter");
    String draftId = randomAlphanumeric(10);
    for (String referenceId : List.of(draftId, randomAlphanumeric(10))) {
      dataAccessRequestDAO.insertDataAccessRequest(
          createDarCollection().getDarCollectionId(),
          referenceId,
          user.getUserId(),
          shared,
          user.getEraCommonsId());
      setDarSubmissionDate(referenceId, null);
    }
    DataAccessRequest edit = new DataAccessRequest();
    edit.setReferenceId(draftId);
    edit.setData(new DataAccessRequestData());
    edit.setDatasetIds(List.of());

    var result = serviceDAO.updateByReferenceId(user, edit, List.of("status"));

    assertEquals(
        List.of(new RemovedDocument(DarDocumentType.COLLABORATION, "shared-letter")),
        result.removedDocuments());
    assertNull(
        dataAccessRequestDAO.findByReferenceId(draftId).getData().getCollaborationLetterLocation());
  }

  // DAAs, dataset and draft files share the bucket; a planted location may name one of them.
  @Test
  void testIsDocumentReferencedFindsAFileStorageObject() {
    User user = createUser();
    String blob = UUID.randomUUID().toString();
    fileStorageObjectDAO.insertNewFile(
        "daa.pdf",
        "DATA_ACCESS_AGREEMENT",
        "gs://bucket/" + blob,
        "application/pdf",
        "1",
        user.getUserId());

    assertTrue(dataAccessRequestDAO.isDocumentReferenced(blob));
    assertFalse(dataAccessRequestDAO.isDocumentReferenced(UUID.randomUUID().toString()));
    // A planted location is compared literally, not as a pattern.
    assertFalse(dataAccessRequestDAO.isDocumentReferenced("%"));
    assertFalse(dataAccessRequestDAO.isDocumentReferenced("planted\\"));
  }

  @Test
  void testInsertDocumentDeletion() {
    User user = createUser();
    dataAccessRequestDAO.insertDocumentDeletion(
        "ref-1", "IRB", "blob-1", "REPLACED", user.getUserId());

    var row =
        jdbi.withHandle(
            h ->
                h.createQuery(
                        """
                        SELECT reference_id, document_type, blob_name, reason, user_id,
                          delete_date IS NOT NULL AS dated
                        FROM dar_document_deletion WHERE reference_id = 'ref-1'
                        """)
                    .mapToMap()
                    .one());
    assertEquals("IRB", row.get("document_type"));
    assertEquals("blob-1", row.get("blob_name"));
    assertEquals("REPLACED", row.get("reason"));
    assertEquals(user.getUserId(), row.get("user_id"));
    assertEquals(true, row.get("dated"));
    // Only known document types and reasons are stored.
    assertThrows(
        Exception.class,
        () -> dataAccessRequestDAO.insertDocumentDeletion("ref-1", "IRB", "blob-2", "OTHER", null));
  }

  // The table's CHECK constraints list the enum names, so a new constant needs a migration. Each
  // combination the service can write must insert.
  @Test
  void testInsertDocumentDeletionAcceptsEveryTypeAndReason() {
    for (DarDocumentType type : DarDocumentType.values()) {
      for (DarDocumentDeletionReason reason : DarDocumentDeletionReason.values()) {
        assertDoesNotThrow(
            () ->
                dataAccessRequestDAO.insertDocumentDeletion(
                    "ref-all", type.name(), "blob", reason.name(), null),
            type + " " + reason);
      }
    }
  }

  @Test
  void transactionRollsBackDataAccessRequestWhenDatasetInsertFails() {
    User user = createUser();
    String referenceId = UUID.randomUUID().toString();
    counterDAO.addCounter(CounterService.DAR_COUNTER, 0);

    assertThrows(
        JdbiException.class,
        () ->
            serviceDAO.inTransaction(
                transactionDAOs -> {
                  transactionDAOs.counterDAO().incrementCountByName(CounterService.DAR_COUNTER);
                  Integer collectionId =
                      transactionDAOs
                          .darCollectionDAO()
                          .insertDarCollection("DAR-ROLLBACK", user.getUserId(), new Date());
                  transactionDAOs
                      .dataAccessRequestDAO()
                      .insertDataAccessRequest(
                          collectionId,
                          referenceId,
                          user.getUserId(),
                          new DataAccessRequestData(),
                          user.getEraCommonsId());
                  transactionDAOs
                      .dataAccessRequestDAO()
                      .insertAllDarDatasets(List.of(new DarDataset(referenceId, -1)));
                  return null;
                }));

    assertNull(dataAccessRequestDAO.findByReferenceId(referenceId));
    assertEquals(1, counterDAO.incrementCountByName(CounterService.DAR_COUNTER));
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(name, user.getUserId(), objectId, dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private DarCollection createDarCollection() {
    User user = createUserWithInstitution();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDatasetId());
    Election cancelled =
        createCancelledAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());
    createFinalVote(user.getUserId(), cancelled.getElectionId());
    createFinalVote(user.getUserId(), access.getElectionId());
    createDataAccessRequest(user.getUserId(), collectionId);
    createDataAccessRequest(user.getUserId(), collectionId);
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private Election createCancelledAccessElection(String referenceId, Integer datasetId) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.CANCELED.getValue(),
            referenceId,
            datasetId);
    return electionDAO.findElectionById(electionId);
  }

  /**
   * Creates a new user, dataset, data access request, and dar collection
   *
   * @return Populated DataAccessRequest
   */
  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
    data.setHmb(true);
    data.setMethods(false);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, userId, data, randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private void createFinalVote(Integer userId, Integer electionId) {
    voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
  }

  private Election createDataAccessElection(String referenceId, Integer datasetId) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            referenceId,
            datasetId);
    return electionDAO.findElectionById(electionId);
  }
}
