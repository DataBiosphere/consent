package org.broadinstitute.consent.http.db;

import static org.broadinstitute.consent.http.service.DataAccessRequestService.EXPIRE_NOTICE_INTERVAL;
import static org.broadinstitute.consent.http.service.DataAccessRequestService.EXPIRE_WARN_INTERVAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.mail.MailMessageInsert;
import org.jdbi.v3.core.JdbiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataAccessRequestDAOTest extends DAOTestHelper {

  private static DataAccessRequestData createDataAccessRequestData() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
    data.setHmb(true);
    data.setMethods(false);
    return data;
  }

  @Test
  void testFindAll() {
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequests();
    assertTrue(dars.isEmpty());

    DataAccessRequest collectionDar = createDataAccessRequestV3();
    DarCollection collection =
        darCollectionDAO.findDARCollectionByCollectionId(collectionDar.getCollectionId());
    createDraftDataAccessRequest();
    List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDataAccessRequests();
    assertFalse(newDars.isEmpty());
    assertEquals(collection.getDars().size(), newDars.size());
  }

  @Test
  void testFindAllDrafts() {
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertTrue(dars.isEmpty());

    createDataAccessRequestV3();
    DataAccessRequest draft = createDraftDataAccessRequest();
    Dataset d1 = createDARDAOTestDataset();
    Dataset d2 = createDARDAOTestDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(draft.getReferenceId(), d1.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(draft.getReferenceId(), d2.getDatasetId());
    List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertFalse(newDars.isEmpty());
    assertEquals(1, newDars.size());
    assertTrue(newDars.getFirst().getDraft());
    assertNull(newDars.getFirst().getDarCode());
    assertFalse(newDars.getFirst().getProgressReport());
  }

  @Test
  void testFindAllDraftsByUserId() {
    DataAccessRequest dar = createDraftDataAccessRequest();
    Dataset d1 = createDARDAOTestDataset();
    Dataset d2 = createDARDAOTestDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDatasetId());

    List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDraftsByUserId(dar.getUserId());
    assertFalse(newDars.isEmpty());
    assertEquals(1, newDars.size());
    assertNull(newDars.getFirst().getDarCode());

    List<DataAccessRequest> missingDars = dataAccessRequestDAO.findAllDraftsByUserId(0);
    assertTrue(missingDars.isEmpty());
  }

  @Test
  void updateDraftToNonDraft() {
    DataAccessRequest dar = createDraftDataAccessRequest();

    List<DataAccessRequest> draftDars1 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertFalse(draftDars1.isEmpty());
    assertEquals(1, draftDars1.size());
    DataAccessRequestData darData = draftDars1.getFirst().getData();
    dataAccessRequestDAO.updateDataByReferenceId(
        dar.referenceId, dar.userId, new Date(), new Date(), darData, randomAlphabetic(10));
    List<DataAccessRequest> draftDars2 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertTrue(draftDars2.isEmpty());
  }

  @Test
  void updateNonDraftToDraft() {
    DataAccessRequest dar = createDataAccessRequestV3();

    List<DataAccessRequest> draftDars1 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertTrue(draftDars1.isEmpty());

    dataAccessRequestDAO.updateDataByReferenceId(
        dar.getReferenceId(), dar.userId, null, new Date(), dar.getData(), randomAlphabetic(10));
    List<DataAccessRequest> draftDars2 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertFalse(draftDars2.isEmpty());
    assertEquals(1, draftDars2.size());
    assertEquals(dar.getDarCode(), draftDars2.getFirst().getDarCode());
  }

  @Test
  void updateDraftToNonDraftByCollectionId() {
    DarCollection darColl = createDarCollection();
    DataAccessRequest dar = new ArrayList<>(darColl.getDars().values()).getFirst();

    dataAccessRequestDAO.updateDataByReferenceId(
        dar.referenceId, dar.userId, null, new Date(), dar.getData(), randomAlphabetic(10));
    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    DataAccessRequestData darData = dar.getData();
    dataAccessRequestDAO.updateDataByReferenceId(
        dar.referenceId, dar.userId, new Date(), new Date(), darData, randomAlphabetic(10));
    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertFalse(dar.getDraft());
    Timestamp expectedTimestamp =
        new Timestamp(
            dar.getSubmissionDate().getTime() + DataAccessRequest.EXPIRATION_DURATION_MILLIS);
    assertEquals(expectedTimestamp, dar.getExpiresAt());
    assertNotNull(dar.getDarCode());
  }

  @Test
  void testDataAccessRequestSubmissionDateAbsent() {
    DarCollection darColl = createDarCollection();
    DataAccessRequest dar = new ArrayList<>(darColl.getDars().values()).getFirst();

    dataAccessRequestDAO.updateDataByReferenceId(
        dar.referenceId, dar.userId, null, new Date(), dar.getData(), randomAlphabetic(10));
    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertNull(dar.getSubmissionDate());
    assertTrue(dar.getDraft());
    assertFalse(dar.getExpired());
    assertNull(dar.getExpiresAt());
  }

  @Test
  void testDataAccessRequestSubmissionDatePresent() {
    DarCollection darColl = createDarCollection();
    DataAccessRequest dar = new ArrayList<>(darColl.getDars().values()).getFirst();

    dataAccessRequestDAO.updateDataByReferenceId(
        dar.referenceId, dar.userId, new Date(), new Date(), dar.getData(), randomAlphabetic(10));
    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertFalse(dar.getDraft());
    assertFalse(dar.getExpired());
    Timestamp expectedTimestamp =
        new Timestamp(
            dar.getSubmissionDate().getTime() + DataAccessRequest.EXPIRATION_DURATION_MILLIS);
    assertEquals(expectedTimestamp, dar.getExpiresAt());
  }

  @Test
  void updateNonDraftToDraftByCollectionId() {
    DarCollection darColl = createDarCollection();
    DataAccessRequest dar = new ArrayList<>(darColl.getDars().values()).getFirst();

    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertFalse(dar.getDraft());
    dataAccessRequestDAO.updateDataByReferenceId(
        dar.referenceId, dar.userId, null, new Date(), dar.getData(), randomAlphabetic(10));
    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertTrue(dar.getDraft());
    assertNull(dar.getSubmissionDate());
    assertFalse(dar.getExpired());
    assertNull(dar.getExpiresAt());
  }

  @Test
  void testCreate() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + randomInt(1, 999999999);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    Dataset d1 = createDARDAOTestDataset();
    Dataset d2 = createDARDAOTestDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDatasetId());
    DataAccessRequest foundDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertNotNull(foundDar);
    assertNotNull(foundDar.getData());
    assertTrue(foundDar.getDatasetIds().contains(d1.getDatasetId()));
    assertTrue(foundDar.getDatasetIds().contains(d2.getDatasetId()));
  }

  @Test
  void testFindByReferenceIdIncludesAdminDarNotes() {
    DataAccessRequest dar = createDataAccessRequestV3();
    String adminDarNote = "Administrative note";
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "UPDATE data_access_request SET admin_dar_notes = :note WHERE reference_id = :referenceId")
                .bind("note", adminDarNote)
                .bind("referenceId", dar.getReferenceId())
                .execute());

    DataAccessRequest foundDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertNotNull(foundDar);
    assertEquals(adminDarNote, foundDar.getAdminDarNotes());
  }

  @Test
  void testFindByReferenceIds() {
    DataAccessRequest dar1 = createDataAccessRequestV3();
    DataAccessRequest dar2 = createDataAccessRequestV3();
    DataAccessRequest dar3 = createDataAccessRequestV3();
    List<String> referenceIds =
        Arrays.asList(dar1.getReferenceId(), dar2.getReferenceId(), dar3.getReferenceId());

    List<DataAccessRequest> dars = dataAccessRequestDAO.findByReferenceIds(referenceIds);
    assertNotNull(dars);
    assertFalse(dars.isEmpty());
    assertEquals(3, dars.size());
    dars.forEach(dar -> assertNotNull(dar.getDarCode()));
    dars.forEach(dar -> assertNull(dar.getParentId()));
  }

  @Test
  void testUpdateByReferenceId() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Date now = new Date();
    User user = createUser();
    String rus = randomAlphabetic(10);
    dar.getData().setRus(rus);
    dar.getData().setValidRestriction(false);
    dataAccessRequestDAO.updateDataByReferenceId(
        dar.getReferenceId(), user.getUserId(), now, now, dar.getData(), randomAlphabetic(10));
    DataAccessRequest updatedDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertEquals(rus, updatedDar.getData().getRus());
    assertFalse(updatedDar.getData().getValidRestriction());
  }

  @Test
  void testUnsupportedUnicodeDarInsert() {
    String unsupportedUnicode = "\u0000";
    DarCollection collection = createDarCollection();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setRus(String.format(" unsupported unicode characters: %s ", unsupportedUnicode));
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collection.getDarCollectionId(),
        referenceId,
        collection.getCreateUserId(),
        now,
        now,
        now,
        data,
        randomAlphabetic(10));
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
    assertNotNull(dar);
    assertFalse(dar.getData().getRus().contains(unsupportedUnicode));
  }

  @Test
  void testUnsupportedUnicodeDarUpdate() {
    String unsupportedUnicode = "\u0000";
    DarCollection collection = createDarCollection();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);
    Date now = new Date();

    String rus = randomAlphabetic(10);
    dar.getData().setRus(rus + String.format(" %s ", unsupportedUnicode));
    dataAccessRequestDAO.updateDataByReferenceId(
        dar.getReferenceId(),
        collection.getCreateUserId(),
        now,
        now,
        dar.getData(),
        randomAlphabetic(10));

    DataAccessRequest updatedDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertNotNull(updatedDar);
    assertFalse(updatedDar.getData().getRus().contains(unsupportedUnicode));
  }

  @Test
  void testUnsupportedUnicodeDraftDar() {
    String unsupportedUnicode = "\u0000";
    User user = createUser();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setRus(String.format(" unsupported unicode characters: %s ", unsupportedUnicode));
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        referenceId, user.getUserId(), now, now, data);
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
    assertNotNull(dar);
    assertFalse(dar.getData().getRus().contains(unsupportedUnicode));
  }

  @Test
  void testInsertDraftDataAccessRequest() {
    DataAccessRequest dar = createDraftDataAccessRequest();
    assertNotNull(dar);
  }

  @Test
  void testInsertVersion3() {
    DataAccessRequest dar = createDataAccessRequestV3();
    assertNotNull(dar);
  }

  @Test
  void testDeleteByCollectionId() {
    // creates a dar with a collection ID (also creates a DarCollection)
    DataAccessRequest dar = createDataAccessRequestV3();
    DataAccessRequest returned = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertNotNull(returned);
    assertEquals(dar.getId(), returned.getId());
    dataAccessRequestDAO.deleteByCollectionId(dar.getCollectionId());
    DataAccessRequest returnedAfter = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertNull(returnedAfter);
  }

  @Test
  void testCancelDeleteByCollectionIds() {
    DataAccessRequest dar1 = createDataAccessRequestV3();
    DataAccessRequest dar2 = createDataAccessRequestV3();

    List<String> referenceIds = new ArrayList<>();
    referenceIds.add(dar1.getReferenceId());
    referenceIds.add(dar2.getReferenceId());

    dataAccessRequestDAO.cancelByReferenceIds(referenceIds);

    DataAccessRequest updatedDar1 = dataAccessRequestDAO.findByReferenceId(dar1.getReferenceId());
    DataAccessRequest updatedDar2 = dataAccessRequestDAO.findByReferenceId(dar2.getReferenceId());

    assertEquals(dar1.getReferenceId(), updatedDar1.getReferenceId());
    assertEquals(dar2.getReferenceId(), updatedDar2.getReferenceId());

    assertEquals("Canceled", updatedDar1.getData().getStatus());
    assertEquals("Canceled", updatedDar2.getData().getStatus());

    assertNotNull(updatedDar1.getData().getHmb());
    assertNotNull(updatedDar2.getData().getHmb());
    assertEquals(dar1.getData().getHmb(), updatedDar1.getData().getHmb());
    assertEquals(dar2.getData().getHmb(), updatedDar2.getData().getHmb());

    assertNotNull(updatedDar1.getData().getMethods());
    assertNotNull(updatedDar2.getData().getMethods());
    assertEquals(dar1.getData().getMethods(), updatedDar1.getData().getMethods());
    assertEquals(dar2.getData().getMethods(), updatedDar2.getData().getMethods());
  }

  @Test
  void testUpdateDraftToSubmittedForCollection() {
    DarCollection collection = createDarCollection();
    DataAccessRequest draft = createDraftDataAccessRequest();
    String referenceId = draft.getReferenceId();
    Integer collectionId = collection.getDarCollectionId();
    dataAccessRequestDAO.updateDraftToSubmittedForCollection(collectionId, referenceId);
    DataAccessRequest updatedDraft = dataAccessRequestDAO.findByReferenceId(referenceId);
    assertFalse(updatedDraft.getDraft());
    assertEquals(collectionId, updatedDraft.getCollectionId());
  }

  @Test
  void testArchiveByReferenceIdsStatusChange() {
    DataAccessRequest dar = createDataAccessRequestV3();
    List<String> referenceIds = List.of(dar.getReferenceId());
    dataAccessRequestDAO.cancelByReferenceIds(referenceIds);
    DataAccessRequest canceledDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());

    assertEquals(dar.getReferenceId(), canceledDar.getReferenceId());
    assertEquals("Canceled", canceledDar.getData().getStatus());
    assertNotNull(canceledDar.getData().getHmb());
    assertEquals(dar.getData().getHmb(), canceledDar.getData().getHmb());
    assertNotNull(canceledDar.getData().getMethods());
    assertEquals(dar.getData().getMethods(), canceledDar.getData().getMethods());
  }

  protected DataAccessRequest createDAR(User user, Dataset dataset, String darCode) {
    return createDAR(user, dataset, darCode, new Timestamp(new Date().getTime()));
  }

  protected DataAccessRequest createExpiredDAR(User user, Dataset dataset, String darCode) {
    Instant now = Instant.now();
    Instant overAYearAgo = now.minus(370, ChronoUnit.DAYS);
    return createDAR(user, dataset, darCode, new Timestamp(overAYearAgo.toEpochMilli()));
  }

  // local method to create a DAR
  protected DataAccessRequest createDAR(
      User user, Dataset dataset, String darCode, Timestamp submissionDate) {
    var now = new Timestamp(new Date().getTime());
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), now);
    DataAccessRequest testDar = new DataAccessRequest();
    testDar.setCollectionId(collectionId);
    testDar.setReferenceId(UUID.randomUUID().toString());
    testDar.setUserId(user.getUserId());
    testDar.setCreateDate(now);
    testDar.setSubmissionDate(submissionDate);
    testDar.setUpdateDate(now);
    DataAccessRequestData contents = new DataAccessRequestData();
    testDar.setData(contents);
    dataAccessRequestDAO.insertDataAccessRequest(
        testDar.getCollectionId(),
        testDar.getReferenceId(),
        testDar.getUserId(),
        testDar.getCreateDate(),
        testDar.getSubmissionDate(),
        testDar.getUpdateDate(),
        testDar.getData(),
        randomAlphabetic(10));
    dataAccessRequestDAO.insertDARDatasetRelation(testDar.getReferenceId(), dataset.getDatasetId());
    return dataAccessRequestDAO.findByReferenceId(testDar.getReferenceId());
  }

  // local method to create a Draft DAR
  protected DataAccessRequest createDraftDAR(User user) {
    Date now = new Date();
    DataAccessRequestData contents = new DataAccessRequestData();
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        referenceId, user.getUserId(), now, now, contents);
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  // findAllDataAccessRequests should exclude archived DARs
  @Test
  void testFindAllArchived() {
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequests();
    assertTrue(dars.isEmpty());

    String darCode = "DAR-" + randomInt(100, 1000);
    Dataset dataset = createDARDAOTestDataset();
    User user = createUserWithInstitution();
    DataAccessRequest testDar = createDAR(user, dataset, darCode);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
    List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllDataAccessRequests();
    assertTrue(returnedDARs.isEmpty());
  }

  // findAllDataAccessRequests should exclude archived DARs
  // test case with two DARs
  @Test
  void testFindAllFilterArchived() {
    User user = createUserWithInstitution();

    String darCode1 = "DAR-" + randomInt(100, 200);
    String darCode2 = "DAR-" + randomInt(201, 300);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();

    DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
    createDAR(user, dataset2, darCode2);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
    List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllDataAccessRequests();
    assertEquals(1, returnedDARs.size());
    assertNotNull(returnedDARs.getFirst().getDarCode());
  }

  // See: https://broadworkbench.atlassian.net/browse/DUOS-2182
  @Test
  void testEnsureOnlyDataAccessRequestsByDatasetIdReturnsJustForSpecificDatasetId() {
    String darCode1 = "DAR-" + randomInt(100, 1000);
    String darCode2 = "DAR-" + randomInt(100, 1000);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();
    User user1 = createUser();
    User user2 = createUser();
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1);
    DataAccessRequest testDar2 = createDAR(user2, dataset2, darCode2);

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), e1.getElectionId());
    Date now = new Date();
    updateVote(true, "", now, v1.getVoteId(), false, e1.getElectionId(), now, false);

    Election e2 = createDataAccessElection(testDar2.getReferenceId(), dataset2.getDatasetId());
    Vote v2 = createVote(dataset2.getCreateUserId(), e2.getElectionId(), VoteType.RADAR_APPROVE);
    now = new Date();
    updateVote(true, "", now, v2.getVoteId(), false, e2.getElectionId(), now, false);

    List<DataAccessRequest> dars =
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId());
    assertEquals(1, dars.size());
    assertTrue(dars.getFirst().getDatasetIds().contains(dataset1.getDatasetId()));
    assertEquals(darCode1, dars.getFirst().getDarCode());

    List<DataAccessRequest> returnedDARs =
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDatasetId());
    assertEquals(1, returnedDARs.size());
    assertTrue(returnedDARs.getFirst().getDatasetIds().contains(dataset2.getDatasetId()));
    assertEquals(darCode2, returnedDARs.getFirst().getDarCode());
  }

  @Test
  void testFindAllApprovedDataAccessRequestsByDatasetId() {
    String darCode1 = "DAR-" + randomInt(100, 1000000);
    String darCode2 = "DAR-" + randomInt(100, 1000000);
    String darCode3 = "DAR-" + randomInt(100, 1000000);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();

    assertTrue(dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).isEmpty());
    assertTrue(dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDatasetId()).isEmpty());

    User user1 = createUserWithInstitution();
    User user2 = createUserWithInstitution();
    User user3 = createUserWithInstitution();
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1);
    DataAccessRequest testDar2 = createDAR(user2, dataset2, darCode2);
    DataAccessRequest testDar3 = createDAR(user3, dataset2, darCode3);
    assertTrue(dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).isEmpty());
    assertTrue(dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDatasetId()).isEmpty());

    assertEquals(
        0, dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDatasetId()).size());

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), e1.getElectionId());
    Date now = new Date();
    updateVote(true, "", now, v1.getVoteId(), false, e1.getElectionId(), now, false);

    Election e2 = createDataAccessElection(testDar2.getReferenceId(), dataset2.getDatasetId());
    Vote v2 = createFinalVote(dataset2.getCreateUserId(), e2.getElectionId());
    now = new Date();
    updateVote(true, "", now, v2.getVoteId(), false, e2.getElectionId(), now, false);

    assertEquals(
        1, dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).size());
    assertEquals(
        testDar1.getUserId(),
        dataAccessRequestDAO
            .findApprovedDARsByDatasetId(dataset1.getDatasetId())
            .getFirst()
            .getUserId());
    assertEquals(
        1, dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDatasetId()).size());
    assertEquals(
        testDar2.getUserId(),
        dataAccessRequestDAO
            .findApprovedDARsByDatasetId(dataset2.getDatasetId())
            .getFirst()
            .getUserId());

    Election e3 = createDataAccessElection(testDar3.getReferenceId(), dataset2.getDatasetId());
    Vote v3 = createFinalVote(dataset2.getCreateUserId(), e3.getElectionId());
    now = new Date();
    updateVote(true, "", now, v3.getVoteId(), false, e3.getElectionId(), now, false);

    List<DataAccessRequest> approvedDars =
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDatasetId());
    List<Integer> approvedDarIds = approvedDars.stream().map(DataAccessRequest::getId).toList();
    assertEquals(2, approvedDarIds.size());
    assertTrue(approvedDarIds.contains(testDar3.getId()));
    assertTrue(approvedDarIds.contains(testDar2.getId()));
    assertFalse(approvedDarIds.contains(testDar1.getId()));
  }

  @Test
  void testFindAllDatasetApprovalsByDar() {
    String darCode1 = "DAR-" + randomInt(100, 1000000);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();
    Dataset dataset3 = createDARDAOTestDataset();

    User user1 = createUserWithInstitution();
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1);
    dataAccessRequestDAO.insertDARDatasetRelation(
        testDar1.getReferenceId(), dataset2.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(
        testDar1.getReferenceId(), dataset3.getDatasetId());

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v1 = createVote(dataset1.getCreateUserId(), e1.getElectionId(), VoteType.RADAR_APPROVE);

    Election e2 = createDataAccessElection(testDar1.getReferenceId(), dataset2.getDatasetId());
    Vote v2 = createFinalVote(dataset2.getCreateUserId(), e2.getElectionId());

    Election e3 = createDataAccessElection(testDar1.getReferenceId(), dataset3.getDatasetId());
    Vote v3 = createFinalVote(dataset3.getCreateUserId(), e3.getElectionId());

    Date now = new Date();

    assertTrue(dataAccessRequestDAO.findDatasetApprovalsByDar(testDar1.getReferenceId()).isEmpty());

    updateVote(true, "", now, v1.getVoteId(), false, e1.getElectionId(), now, false);

    Set<Integer> approvedDatasetIds =
        dataAccessRequestDAO.findDatasetApprovalsByDar(testDar1.getReferenceId());
    assertEquals(1, approvedDatasetIds.size());
    assertTrue(approvedDatasetIds.contains(dataset1.getDatasetId()));

    updateVote(true, "", now, v2.getVoteId(), false, e2.getElectionId(), now, false);

    approvedDatasetIds = dataAccessRequestDAO.findDatasetApprovalsByDar(testDar1.getReferenceId());

    assertEquals(2, approvedDatasetIds.size());
    assertTrue(approvedDatasetIds.contains(dataset1.getDatasetId()));
    assertTrue(approvedDatasetIds.contains(dataset2.getDatasetId()));

    updateVote(false, "", now, v3.getVoteId(), false, e3.getElectionId(), now, false);

    approvedDatasetIds = dataAccessRequestDAO.findDatasetApprovalsByDar(testDar1.getReferenceId());

    assertEquals(2, approvedDatasetIds.size());
    assertTrue(approvedDatasetIds.contains(dataset1.getDatasetId()));
    assertTrue(approvedDatasetIds.contains(dataset2.getDatasetId()));
  }

  @Test
  void testFindAllDatasetApprovalsByDars_IncludesExpired() {
    String darCode1 = "DAR-" + randomInt(100, 1000000);
    Dataset dataset1 = createDARDAOTestDataset();

    User user1 = createUserWithInstitution();
    DataAccessRequest testDar1 = createExpiredDAR(user1, dataset1, darCode1);
    dataAccessRequestDAO.insertDARDatasetRelation(
        testDar1.getReferenceId(), dataset1.getDatasetId());

    Election election =
        createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), election.getElectionId());

    Date now = new Date();

    updateVote(true, "", now, v1.getVoteId(), false, election.getElectionId(), now, false);

    Set<Integer> approvedDatasetIds =
        dataAccessRequestDAO.findDatasetApprovalsByDar(testDar1.getReferenceId());
    assertEquals(1, approvedDatasetIds.size());
    assertTrue(approvedDatasetIds.contains(dataset1.getDatasetId()));
  }

  @Test
  void testFindDatasetApprovalsByDAR_ExcludeCloseouts() {
    // Create a dar collection
    User user = createUserWithInstitution();
    String darCode = "DAR-" + randomInt(1, 10000);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());

    // Create an approved DAR on a dataset
    Dataset dataset = createDataset();
    DataAccessRequest parentDAR = createDataAccessRequest(user.getUserId(), collectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(
        parentDAR.getReferenceId(), dataset.getDatasetId());
    Election election =
        createDataAccessElection(parentDAR.getReferenceId(), dataset.getDatasetId());
    Vote vote = createFinalVote(dataset.getCreateUserId(), election.getElectionId());
    Date now = new Date();
    updateVote(true, "", now, vote.getVoteId(), false, election.getElectionId(), now, false);
    // Ensure we can find the approved dataset for the parent DAR
    Set<Integer> approvedDatasetIds =
        dataAccessRequestDAO.findDatasetApprovalsByDar(parentDAR.getReferenceId());
    assertTrue(approvedDatasetIds.contains(dataset.getDatasetId()));

    // Create a closeout DAR for the parent DAR
    DataAccessRequest closeoutDAR =
        createProgressReport(
            user.getEraCommonsId(), user.getUserId(), collectionId, parentDAR.getId());
    CloseoutSupplement closeout =
        new CloseoutSupplement(List.of("Reason"), "Other Reason", user.getUserId());
    closeoutDAR.getData().setCloseoutSupplement(closeout);
    dataAccessRequestDAO.updateDataByReferenceId(
        closeoutDAR.getReferenceId(),
        user.getUserId(),
        now,
        now,
        closeoutDAR.getData(),
        randomAlphabetic(10));

    // Ensure that we do NOT get the dataset from the parent DAR
    Set<Integer> noApprovedDatasetIds1 =
        dataAccessRequestDAO.findDatasetApprovalsByDar(parentDAR.getReferenceId());
    assertTrue(
        noApprovedDatasetIds1.isEmpty(), "Parent DAR should not be included in dataset approvals");

    // Ensure that we do NOT get the dataset from the closeout DAR
    Set<Integer> noApprovedDatasetIds2 =
        dataAccessRequestDAO.findDatasetApprovalsByDar(closeoutDAR.getReferenceId());
    assertTrue(
        noApprovedDatasetIds2.isEmpty(),
        "Closeout DAR should not be included in dataset approvals");
  }

  /**
   * The final-vote window is partitioned by reference id AND dataset id. Partitioning by dataset id
   * alone lets the most recent final vote anywhere in the system decide every DAR that touches that
   * dataset, so an unrelated denial silently revokes approvals that were granted correctly.
   */
  @Test
  void testFindDatasetApprovalsByDar_OtherDarDenialDoesNotRevokeApproval() {
    Dataset dataset = createDARDAOTestDataset();
    User user = createUserWithInstitution();
    Date earlier = new Date(Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli());
    Date later = new Date();

    DataAccessRequest approvedDar = createDAR(user, dataset, "DAR-" + randomInt(100, 1000000));
    Election approvedElection =
        createDataAccessElection(approvedDar.getReferenceId(), dataset.getDatasetId());
    Vote approvedVote =
        createFinalVote(dataset.getCreateUserId(), approvedElection.getElectionId());
    updateVote(
        true,
        "",
        earlier,
        approvedVote.getVoteId(),
        false,
        approvedElection.getElectionId(),
        earlier,
        false);

    assertTrue(
        dataAccessRequestDAO
            .findDatasetApprovalsByDar(approvedDar.getReferenceId())
            .contains(dataset.getDatasetId()));

    // A separate DAR on the same dataset is denied more recently.
    DataAccessRequest deniedDar = createDAR(user, dataset, "DAR-" + randomInt(100, 1000000));
    Election deniedElection =
        createDataAccessElection(deniedDar.getReferenceId(), dataset.getDatasetId());
    Vote deniedVote = createFinalVote(dataset.getCreateUserId(), deniedElection.getElectionId());
    updateVote(
        false,
        "",
        later,
        deniedVote.getVoteId(),
        false,
        deniedElection.getElectionId(),
        later,
        false);

    assertTrue(
        dataAccessRequestDAO
            .findDatasetApprovalsByDar(approvedDar.getReferenceId())
            .contains(dataset.getDatasetId()),
        "A denial on a different DAR must not revoke this DAR's approval");
    assertTrue(
        dataAccessRequestDAO.findDatasetApprovalsByDar(deniedDar.getReferenceId()).isEmpty(),
        "The denied DAR must have no approved datasets");
  }

  /**
   * The inverse of {@link #testFindDatasetApprovalsByDar_OtherDarDenialDoesNotRevokeApproval()}: an
   * approval granted to someone else must not confer access on a DAR that was denied.
   */
  @Test
  void testFindDatasetApprovalsByDar_OtherDarApprovalDoesNotGrantApproval() {
    Dataset dataset = createDARDAOTestDataset();
    User user = createUserWithInstitution();
    Date earlier = new Date(Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli());
    Date later = new Date();

    DataAccessRequest deniedDar = createDAR(user, dataset, "DAR-" + randomInt(100, 1000000));
    Election deniedElection =
        createDataAccessElection(deniedDar.getReferenceId(), dataset.getDatasetId());
    Vote deniedVote = createFinalVote(dataset.getCreateUserId(), deniedElection.getElectionId());
    updateVote(
        false,
        "",
        earlier,
        deniedVote.getVoteId(),
        false,
        deniedElection.getElectionId(),
        earlier,
        false);

    // A separate DAR on the same dataset is approved more recently.
    DataAccessRequest approvedDar = createDAR(user, dataset, "DAR-" + randomInt(100, 1000000));
    Election approvedElection =
        createDataAccessElection(approvedDar.getReferenceId(), dataset.getDatasetId());
    Vote approvedVote =
        createFinalVote(dataset.getCreateUserId(), approvedElection.getElectionId());
    updateVote(
        true,
        "",
        later,
        approvedVote.getVoteId(),
        false,
        approvedElection.getElectionId(),
        later,
        false);

    assertTrue(
        dataAccessRequestDAO.findDatasetApprovalsByDar(deniedDar.getReferenceId()).isEmpty(),
        "An approval on a different DAR must not grant this DAR access");
    assertTrue(
        dataAccessRequestDAO
            .findDatasetApprovalsByDar(approvedDar.getReferenceId())
            .contains(dataset.getDatasetId()));
  }

  /**
   * Reproduces the production shape: a DAR approved on two datasets loses only the dataset that
   * some other DAR later had denied, so the call returns one id instead of two.
   */
  @Test
  void testFindDatasetApprovalsByDar_MultipleDatasetsSurviveOtherDarDenial() {
    Dataset sharedDataset = createDARDAOTestDataset();
    Dataset otherDataset = createDARDAOTestDataset();
    User user = createUserWithInstitution();
    Date earlier = new Date(Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli());
    Date later = new Date();

    DataAccessRequest dar = createDAR(user, sharedDataset, "DAR-" + randomInt(100, 1000000));
    dataAccessRequestDAO.insertDARDatasetRelation(
        dar.getReferenceId(), otherDataset.getDatasetId());

    for (Dataset dataset : List.of(sharedDataset, otherDataset)) {
      Election election = createDataAccessElection(dar.getReferenceId(), dataset.getDatasetId());
      Vote vote = createFinalVote(dataset.getCreateUserId(), election.getElectionId());
      updateVote(
          true, "", earlier, vote.getVoteId(), false, election.getElectionId(), earlier, false);
    }

    assertEquals(2, dataAccessRequestDAO.findDatasetApprovalsByDar(dar.getReferenceId()).size());

    // An unrelated DAR is denied on just one of the two datasets, more recently.
    DataAccessRequest unrelatedDar =
        createDAR(user, sharedDataset, "DAR-" + randomInt(100, 1000000));
    Election unrelatedElection =
        createDataAccessElection(unrelatedDar.getReferenceId(), sharedDataset.getDatasetId());
    Vote unrelatedVote =
        createFinalVote(sharedDataset.getCreateUserId(), unrelatedElection.getElectionId());
    updateVote(
        false,
        "",
        later,
        unrelatedVote.getVoteId(),
        false,
        unrelatedElection.getElectionId(),
        later,
        false);

    Set<Integer> approvals = dataAccessRequestDAO.findDatasetApprovalsByDar(dar.getReferenceId());
    assertEquals(
        2, approvals.size(), "Both approved datasets must survive an unrelated DAR's denial");
    assertTrue(approvals.contains(sharedDataset.getDatasetId()));
    assertTrue(approvals.contains(otherDataset.getDatasetId()));
  }

  /**
   * Tests the case where a user has been approved for access, then denied access, and that the user
   * does not show up as an approved user for the dataset.
   */
  @Test
  void testFindAllApprovedDataAccessRequestsByDatasetId_ApprovedThenDeniedCase() {
    String darCode1 = "DAR-" + randomInt(100, 1000000);
    Dataset dataset1 = createDARDAOTestDataset();
    User user1 = createUserWithInstitution();
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1);

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), e1.getElectionId());
    Date now = new Date();
    updateVote(true, "", now, v1.getVoteId(), false, e1.getElectionId(), now, false);

    assertEquals(
        1, dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).size());

    Election e2 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v2 = createFinalVote(dataset1.getCreateUserId(), e2.getElectionId());
    now = new Date();
    updateVote(false, "", now, v2.getVoteId(), false, e2.getElectionId(), now, false);

    assertEquals(
        0, dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).size());
  }

  /**
   * Tests the case where a user has been approved for access, then a new election is created, and
   * that the user still has access until a final no vote is recorded.
   */
  @Test
  void testFindAllApprovedDataAccessRequestsByDatasetId_ApprovedThenNewElectionCase() {
    String darCode1 = "DAR-" + randomInt(100, 1000000);
    Dataset dataset1 = createDARDAOTestDataset();
    User user1 = createUserWithInstitution();
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1);

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), e1.getElectionId());
    Date now = new Date();
    updateVote(true, "", now, v1.getVoteId(), false, e1.getElectionId(), now, false);

    assertEquals(
        1, dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).size());

    Election e2 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v2 = createVote(dataset1.getCreateUserId(), e2.getElectionId(), VoteType.FINAL);

    assertEquals(
        1, dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).size());

    updateVote(false, "", now, v2.getVoteId(), false, e2.getElectionId(), now, false);
    assertEquals(
        0, dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).size());
  }

  @ParameterizedTest
  @ValueSource(longs = {200, 370})
  void testFindAllApprovedDataAccessRequestsByDatasetId_ExpiredDARCase(long submissionDaysAgo) {
    String darCode1 = "DAR-" + randomInt(100, 1000000);
    Dataset dataset1 = createDARDAOTestDataset();
    User user1 = createUserWithInstitution();
    var submissionDate =
        new Timestamp(new Date().getTime() - TimeUnit.DAYS.toMillis(submissionDaysAgo));
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1, submissionDate);

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), e1.getElectionId());
    Date now = new Date();
    updateVote(true, "", now, v1.getVoteId(), false, e1.getElectionId(), now, false);

    var expectedDARS = 1;
    // If submission date is more than a year ago, then the DAR should not be returned from
    // findApprovedDARsByDatasetId
    if (submissionDaysAgo >= 365) {
      expectedDARS = 0;
    }

    assertEquals(
        expectedDARS,
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).size());
  }

  @Test
  void testFindAllApprovedDataAccessRequestsByDatasetId_NullSubmissionDate() {
    String darCode1 = "DAR-" + randomInt(100, 1000000);
    Dataset dataset1 = createDARDAOTestDataset();
    User user1 = createUserWithInstitution();
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1, null);

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDatasetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), e1.getElectionId());
    Date now = new Date();
    updateVote(true, "", now, v1.getVoteId(), false, e1.getElectionId(), now, false);

    // If submission date is null, then the DAR should not be returned from
    // findApprovedDARsByDatasetId
    assertEquals(
        0, dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDatasetId()).size());
  }

  @Test
  void testFindApprovedDARsByDatasetId_ExcludeCloseouts() {
    // Create a dar collection
    User user = createUserWithInstitution();
    String darCode = "DAR-" + randomInt(1, 10000);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());

    // Create an approved DAR on a dataset
    Dataset dataset = createDataset();
    DataAccessRequest parentDAR = createDataAccessRequest(user.getUserId(), collectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(
        parentDAR.getReferenceId(), dataset.getDatasetId());
    Election election =
        createDataAccessElection(parentDAR.getReferenceId(), dataset.getDatasetId());
    Vote vote = createFinalVote(dataset.getCreateUserId(), election.getElectionId());
    Date now = new Date();
    updateVote(true, "", now, vote.getVoteId(), false, election.getElectionId(), now, false);
    // Ensure we can find the parent DAR for the approved dataset
    List<DataAccessRequest> approvedDARs =
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset.getDatasetId());
    assertEquals(1, approvedDARs.size());
    assertEquals(parentDAR.getReferenceId(), approvedDARs.getFirst().getReferenceId());

    // Create a closeout DAR from the parent DAR
    DataAccessRequest closeoutDAR =
        createProgressReport(
            user.getEraCommonsId(), user.getUserId(), collectionId, parentDAR.getId());
    CloseoutSupplement closeout =
        new CloseoutSupplement(List.of("Reason"), "Other Reason", user.getUserId());
    closeoutDAR.getData().setCloseoutSupplement(closeout);
    dataAccessRequestDAO.updateDataByReferenceId(
        closeoutDAR.getReferenceId(),
        user.getUserId(),
        now,
        now,
        closeoutDAR.getData(),
        randomAlphabetic(10));

    // Ensure we CANNOT find any DARs for the approved dataset
    List<DataAccessRequest> noApprovedDARs =
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset.getDatasetId());
    assertTrue(noApprovedDARs.isEmpty());
  }

  @Test
  void testFindSummaryMetricApprovedDARsByDatasetIdIncludesExpired() {
    // Create a dataset to request access to
    Dataset dataset = createDataset();

    // Create a dar collection
    User user = createUserWithInstitution();
    Date now = new Date();

    // Approved collection: an approved DAR, a progress report on it, and a closeout supplement
    Integer approvedCollectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + randomInt(1, 10), user.getUserId(), new Date());

    // Create an approved DAR on a dataset
    DataAccessRequest approvedDAR = createDataAccessRequest(user.getUserId(), approvedCollectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(
        approvedDAR.getReferenceId(), dataset.getDatasetId());
    Election election =
        createDataAccessElection(approvedDAR.getReferenceId(), dataset.getDatasetId());
    Vote vote = createFinalVote(dataset.getCreateUserId(), election.getElectionId());
    updateVote(true, "", now, vote.getVoteId(), false, election.getElectionId(), now, false);

    // Create a Progress Report on the approved DAR
    DataAccessRequest prDAR = createDataAccessRequest(user.getUserId(), approvedCollectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(prDAR.getReferenceId(), dataset.getDatasetId());
    Election prElection = createDataAccessElection(prDAR.getReferenceId(), dataset.getDatasetId());
    Vote prVote = createFinalVote(dataset.getCreateUserId(), prElection.getElectionId());
    updateVote(true, "", now, prVote.getVoteId(), false, prElection.getElectionId(), now, false);

    // Create a closeout DAR from the PR DAR
    DataAccessRequest closeoutDAR =
        createProgressReport(
            user.getEraCommonsId(), user.getUserId(), approvedCollectionId, prDAR.getId());
    dataAccessRequestDAO.insertDARDatasetRelation(
        closeoutDAR.getReferenceId(), dataset.getDatasetId());
    CloseoutSupplement closeout =
        new CloseoutSupplement(List.of("Reason"), "Other Reason", user.getUserId());
    closeoutDAR.getData().setCloseoutSupplement(closeout);
    dataAccessRequestDAO.updateDataByReferenceId(
        closeoutDAR.getReferenceId(),
        user.getUserId(),
        now,
        now,
        closeoutDAR.getData(),
        randomAlphabetic(10));

    // Unsubmitted draft DAR linked to the dataset — should never source a summary
    DataAccessRequest unsubmittedDAR = createDraftDataAccessRequest();
    dataAccessRequestDAO.insertDARDatasetRelation(
        unsubmittedDAR.getReferenceId(), dataset.getDatasetId());

    // Expired collection: an approved DAR submitted two years ago, still included in metrics
    Integer expiredCollectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + randomInt(11, 20), user.getUserId(), new Date());
    String expiredReferenceId = UUID.randomUUID().toString();
    Timestamp expiredSubmissionDate = Timestamp.from(Instant.now().minus(730, ChronoUnit.DAYS));
    dataAccessRequestDAO.insertDataAccessRequest(
        expiredCollectionId,
        expiredReferenceId,
        user.getUserId(),
        expiredSubmissionDate,
        expiredSubmissionDate,
        expiredSubmissionDate,
        createDataAccessRequestData(),
        randomAlphabetic(10));
    dataAccessRequestDAO.insertDARDatasetRelation(expiredReferenceId, dataset.getDatasetId());
    Integer expiredElectionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            expiredSubmissionDate,
            expiredReferenceId,
            dataset.getDatasetId());
    Vote expiredVote = createFinalVote(dataset.getCreateUserId(), expiredElectionId);
    updateVote(
        true,
        "",
        now,
        expiredVote.getVoteId(),
        false,
        expiredElectionId,
        expiredSubmissionDate,
        false);

    List<DarMetricsSummary> summaries =
        dataAccessRequestDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(
            dataset.getDatasetId());

    // One summary per qualifying collection
    assertFalse(summaries.isEmpty());
    assertEquals(2, summaries.size());

    // The unsubmitted draft never sources a summary
    assertTrue(
        summaries.stream()
            .map(DarMetricsSummary::referenceId)
            .noneMatch(unsubmittedDAR.getReferenceId()::equals));

    // Expired collection summary is present, sourced from its only DAR, and marked expired
    DarMetricsSummary expiredSummary =
        summaries.stream()
            .filter(s -> expiredReferenceId.equals(s.referenceId()))
            .findFirst()
            .orElseThrow();
    assertTrue(expiredSummary.expired());

    // Approved collection summary is present, sourced from one of its submitted DARs, not expired
    DarMetricsSummary approvedSummary =
        summaries.stream()
            .filter(s -> !expiredReferenceId.equals(s.referenceId()))
            .findFirst()
            .orElseThrow();
    assertTrue(
        List.of(approvedDAR.getReferenceId(), prDAR.getReferenceId(), closeoutDAR.getReferenceId())
            .contains(approvedSummary.referenceId()));
    assertFalse(approvedSummary.expired());
  }

  // A collection's most recently submitted DAR may target a different dataset than the one that
  // qualified the collection. The summary must still be returned for the queried dataset, sourced
  // from the latest DAR linked to THAT dataset, not the collection-wide latest.
  @Test
  void testFindSummaryMetricApprovedDARsByDatasetId_latestDarOnDifferentDataset() {
    Dataset queriedDataset = createDataset();
    Dataset otherDataset = createDataset();
    User user = createUserWithInstitution();
    Date now = new Date();

    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + randomInt(1, 10), user.getUserId(), new Date());

    // Earlier DAR linked to the queried dataset and approved — this qualifies the collection
    DataAccessRequest approvedDAR =
        createDataAccessRequest(
            collectionId, user.getUserId(), Date.from(Instant.now().minus(10, ChronoUnit.DAYS)));
    dataAccessRequestDAO.insertDARDatasetRelation(
        approvedDAR.getReferenceId(), queriedDataset.getDatasetId());
    Election election =
        createDataAccessElection(approvedDAR.getReferenceId(), queriedDataset.getDatasetId());
    Vote vote = createFinalVote(queriedDataset.getCreateUserId(), election.getElectionId());
    updateVote(true, "", now, vote.getVoteId(), false, election.getElectionId(), now, false);

    // Later DAR in the same collection, linked only to a different dataset
    DataAccessRequest laterDar = createDataAccessRequest(collectionId, user.getUserId(), now);
    dataAccessRequestDAO.insertDARDatasetRelation(
        laterDar.getReferenceId(), otherDataset.getDatasetId());

    List<DarMetricsSummary> summaries =
        dataAccessRequestDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(
            queriedDataset.getDatasetId());

    // The collection is still returned, sourced from the dataset-linked DAR, not the later one
    assertEquals(1, summaries.size());
    assertEquals(approvedDAR.getReferenceId(), summaries.getFirst().referenceId());
  }

  // findAllDraftDataAccessRequests should exclude archived DARs
  @Test
  void testFindAllDraftsArchived() {
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertTrue(dars.isEmpty());

    User user = createUserWithInstitution();
    DataAccessRequest testDar = createDraftDAR(user);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
    List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertTrue(returnedDARs.isEmpty());
  }

  // findAllDraftsByUserId should exclude archived DARs
  @Test
  void testFindAllDraftsByUserIdArchived() {
    User user = createUserWithInstitution();

    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDraftsByUserId(user.getUserId());
    assertTrue(dars.isEmpty());

    DataAccessRequest testDar = createDraftDAR(user);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
    List<DataAccessRequest> returnedDARs =
        dataAccessRequestDAO.findAllDraftsByUserId(user.getUserId());
    assertTrue(returnedDARs.isEmpty());
  }

  // findByReferenceId should exclude archived DARs
  @Test
  void testFindByReferenceIdArchived() {
    String darCode = "DAR-" + randomInt(100, 1000);
    Dataset dataset = createDARDAOTestDataset();
    User user = createUserWithInstitution();
    DataAccessRequest testDar = createDAR(user, dataset, darCode);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
    DataAccessRequest returnedDAR =
        dataAccessRequestDAO.findByReferenceId(testDar.getReferenceId());
    assertNull(returnedDAR);
  }

  // findByReferenceIds should exclude archived DARs
  @Test
  void testFindByReferenceIdsArchived() {
    String darCode1 = "DAR-" + randomInt(100, 200);
    String darCode2 = "DAR-" + randomInt(201, 300);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();
    User user = createUserWithInstitution();
    DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
    DataAccessRequest testDar2 = createDAR(user, dataset2, darCode2);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar2.getReferenceId()));
    List<DataAccessRequest> returnedDAR =
        dataAccessRequestDAO.findByReferenceIds(
            List.of(testDar1.getReferenceId(), testDar2.getReferenceId()));
    assertTrue(returnedDAR.isEmpty());
  }

  @Test
  void createProgressReport() {
    DarCollection darCollection = createDarCollection();
    DataAccessRequest dar = new ArrayList<>(darCollection.getDars().values()).getFirst();
    DataAccessRequest progressReport =
        createProgressReport(
            createUser().getEraCommonsId(),
            dar.getUserId(),
            darCollection.getDarCollectionId(),
            dar.getId());

    assertNotNull(progressReport);
    assertEquals(dar.getId(), progressReport.getParentId());
    assertEquals(dar.getCollectionId(), progressReport.getCollectionId());
    assertEquals(dar.getUserId(), progressReport.getUserId());
    assertNotNull(progressReport.getData());
    assertNotNull(progressReport.getCreateDate());
    assertNotNull(progressReport.getSubmissionDate());
    assertNotNull(progressReport.getUpdateDate());
    assertNotEquals(dar.getReferenceId(), progressReport.getReferenceId());
    assertTrue(progressReport.getProgressReport());
  }

  @Test
  void insertProgressReport_WithExistingProgressReport() {
    DarCollection darCollection = createDarCollection();
    DataAccessRequest dar = darCollection.getDars().values().stream().findFirst().orElseThrow();
    String eraCommonsId = dar.getEraCommonsId();
    Integer userId = dar.getUserId();
    Integer darCollectionId = darCollection.getDarCollectionId();
    Integer id = dar.getId();
    DataAccessRequest progressReport =
        createProgressReport(dar.getEraCommonsId(), userId, darCollectionId, id);
    assertNotNull(progressReport);

    // Insert of second progress report should fail.
    assertThrows(
        JdbiException.class, () -> createProgressReport(eraCommonsId, userId, darCollectionId, id));

    // Check that the first progress report is not updated.
    DataAccessRequest firstProgressReport =
        dataAccessRequestDAO.findByReferenceId(progressReport.getReferenceId());
    assertEquals(progressReport.id, firstProgressReport.id);
  }

  @Test
  void testFindAgedDARsByEmailTypeOlderThanInterval() {
    User userOne = createUserWithInstitution();
    Integer userOneId = userOne.getUserId();
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userOneId);
    createDataAccessRequest(
        collectionOneId,
        userOneId,
        new Date(Instant.now().minus(360, ChronoUnit.DAYS).toEpochMilli()));
    createDataAccessRequest(
        collectionTwoId,
        userOneId,
        new Date(Instant.now().minus(15, ChronoUnit.DAYS).toEpochMilli()));

    List<DataAccessRequest> dars =
        dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
            EmailType.DAR_EXPIRATION_REMINDER.getTypeInt(),
            EXPIRE_WARN_INTERVAL,
            Timestamp.from(Instant.now().minus(365, ChronoUnit.DAYS)));
    assertFalse(dars.isEmpty());
    assertEquals(1, dars.size());
  }

  @Test
  void testFindAgedDARsByEmailTypeOlderThanIntervalSkipsEntriesBeforeNotBefore() {
    User userOne = createUserWithInstitution();
    Integer userOneId = userOne.getUserId();
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userOneId);
    createDataAccessRequest(
        collectionOneId, userOneId, Date.from(Instant.now().minus(360, ChronoUnit.DAYS)));
    createDataAccessRequest(
        collectionTwoId, userOneId, Date.from(Instant.now().minus(15, ChronoUnit.DAYS)));

    List<DataAccessRequest> dars =
        dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
            EmailType.DAR_EXPIRATION_REMINDER.getTypeInt(),
            EXPIRE_WARN_INTERVAL,
            Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)));
    assertTrue(dars.isEmpty());
  }

  @Test
  void testFindAgedDARsByEmailTypeOlderThanIntervalNoneInRange() {
    DarCollection darCollection = createDarCollection();
    darCollection
        .getDars()
        .keySet()
        .forEach(
            referenceId ->
                dataAccessRequestDAO.updateDraftToSubmittedForCollection(
                    darCollection.getDarCollectionId(), referenceId));
    // query far enough into the past so slight clock variations do not matter for this test
    List<DataAccessRequest> dars =
        dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
            EmailType.DAR_EXPIRATION_REMINDER.getTypeInt(),
            EXPIRE_WARN_INTERVAL,
            Timestamp.from(Instant.now().minus(365, ChronoUnit.DAYS)));
    assertTrue(dars.isEmpty());
  }

  @Test
  void testFindAgedDARsByEmailTypeOlderThanIntervalWithExpiringEntries() {
    User userOne = createUserWithInstitution();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = createUserWithInstitution().getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne =
        createDataAccessRequest(
            collectionOneId, userOneId, Date.from(Instant.now().minus(365, ChronoUnit.DAYS)));
    DataAccessRequest darTwo = createDataAccessRequest(collectionTwoId, userTwoId, new Date());

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(
        darTwo.getReferenceId(), datasetTwo.getDatasetId());

    List<DataAccessRequest> dars =
        dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
            EmailType.DAR_EXPIRATION_REMINDER.getTypeInt(),
            EXPIRE_WARN_INTERVAL,
            Timestamp.from(Instant.now().minus(366, ChronoUnit.DAYS)));

    assertNotNull(dars);
    assertEquals(1, dars.size());
    dars.forEach(
        dar -> {
          assertEquals(userOne.getUserId(), dar.getUserId());
          assertEquals(darOne.getReferenceId(), dar.getReferenceId());
          assertNotNull(dar.getDarCode());
        });
  }

  @Test
  void testFindAgedDARsByEmailTypeOlderThanIntervalWithExpiringEntriesFromMultipleUsers() {
    Integer userOneId = createUserWithInstitution().getUserId();
    Integer userTwoId = createUserWithInstitution().getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Dataset datasetThree = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne =
        createDataAccessRequest(
            collectionOneId, userOneId, Date.from(Instant.now().minus(365, ChronoUnit.DAYS)));
    DataAccessRequest darTwo =
        createDataAccessRequest(
            collectionTwoId, userTwoId, Date.from(Instant.now().minus(350, ChronoUnit.DAYS)));

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(
        darTwo.getReferenceId(), datasetTwo.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(
        darTwo.getReferenceId(), datasetThree.getDatasetId());

    List<DataAccessRequest> dars =
        dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
            EmailType.DAR_EXPIRATION_REMINDER.getTypeInt(),
            EXPIRE_WARN_INTERVAL,
            Timestamp.from(Instant.now().minus(366, ChronoUnit.DAYS)));

    assertNotNull(dars);
    assertEquals(2, dars.size());
    dars.forEach(
        dar -> {
          assertNotNull(dar.getUserId());
          assertNotNull(dar.getReferenceId());
          assertNotNull(dar.getExpiresAt());
          assertNotNull(dar.getDarCode());
        });
  }

  @Test
  void testFindAgedDARsByEmailTypeOlderThanIntervalWithNoExpiringEntries() {
    User userOne = createUserWithInstitution();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = createUserWithInstitution().getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne =
        createDataAccessRequest(
            collectionOneId, userOneId, Date.from(Instant.now().minus(30, ChronoUnit.DAYS)));
    DataAccessRequest darTwo =
        createDataAccessRequest(
            collectionTwoId, userTwoId, Date.from(Instant.now().minus(30, ChronoUnit.DAYS)));

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(
        darTwo.getReferenceId(), datasetTwo.getDatasetId());

    List<DataAccessRequest> dars =
        dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
            EmailType.DAR_EXPIRATION_REMINDER.getTypeInt(),
            EXPIRE_WARN_INTERVAL,
            Timestamp.from(Instant.now().minus(366, ChronoUnit.DAYS)));

    assertNotNull(dars);
    assertEquals(0, dars.size());
  }

  @Test
  void testFindAgedDARsByEmailTypeOlderThanIntervalExpiringEntriesDoesNotRepeatIfEmailSent() {
    User userOne = createUserWithInstitution();
    User userTwo = createUserWithInstitution();
    Integer userOneId = userOne.getUserId();
    Integer userTwoId = userTwo.getUserId();

    Dataset dataset = createDataset(userOneId);
    Dataset datasetTwo = createDataset(userTwoId);
    Dataset datasetThree = createDataset(userTwoId);
    Integer collectionOneId = createDarCollection(userOneId);
    Integer collectionTwoId = createDarCollection(userTwoId);
    DataAccessRequest darOne =
        createDataAccessRequest(
            collectionOneId, userOneId, Date.from(Instant.now().minus(365, ChronoUnit.DAYS)));
    DataAccessRequest darTwo =
        createDataAccessRequest(
            collectionTwoId, userTwoId, Date.from(Instant.now().minus(366, ChronoUnit.DAYS)));

    dataAccessRequestDAO.insertDARDatasetRelation(darOne.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(
        darTwo.getReferenceId(), datasetTwo.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(
        darTwo.getReferenceId(), datasetThree.getDatasetId());
    mailMessageDAO.insert(
        new MailMessageInsert(
            darOne.getReferenceId(),
            null,
            userOneId,
            EmailType.DAR_EXPIRATION_REMINDER.getTypeInt(),
            Date.from(Instant.now()),
            "hello world!",
            "success",
            200));
    List<DataAccessRequest> dars =
        dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
            EmailType.DAR_EXPIRATION_REMINDER.getTypeInt(),
            EXPIRE_WARN_INTERVAL,
            Timestamp.from(Instant.now().minus(367, ChronoUnit.DAYS)));

    assertNotNull(dars);
    assertEquals(1, dars.size());
    dars.forEach(
        dar -> {
          assertEquals(userTwo.getUserId(), dar.getUserId());
          assertEquals(darTwo.getReferenceId(), dar.getReferenceId());
          assertNotNull(dar.getDarCode());
        });

    List<DataAccessRequest> expiredDars =
        dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
            EmailType.DAR_EXPIRED.getTypeInt(),
            EXPIRE_NOTICE_INTERVAL,
            Timestamp.from(Instant.now().minus(367, ChronoUnit.DAYS)));
    assertNotNull(expiredDars);
    assertEquals(2, expiredDars.size());
  }

  @Test
  void testMarkSOApprovalOfCloseout() {
    User user = createUserWithInstitution();
    User signingOfficial = createUserWithInstitution();
    Dataset dataset = createDataset(user.getUserId());
    Integer collection = createDarCollection(user.getUserId());
    DataAccessRequest darToStore =
        createDataAccessRequest(collection, user.getUserId(), Date.from(Instant.now()));
    dataAccessRequestDAO.insertDARDatasetRelation(
        darToStore.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.updateDarApprovalSO(
        signingOfficial.getUserId(), darToStore.getReferenceId());

    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(darToStore.getReferenceId());

    assertEquals(dar.getApprovingSigningOfficialUserId(), signingOfficial.getUserId());
    assertNotNull(dar.getApprovingSigningOfficialApprovedDate());
  }

  @Test
  void testMarkSOApprovalOfDAR() {
    User user = createUserWithInstitution();
    User signingOfficial = createUserWithInstitution();
    Dataset dataset = createDataset(user.getUserId());
    Integer collection = createDarCollection(user.getUserId());
    DataAccessRequest darToStore =
        createDataAccessRequest(collection, user.getUserId(), Date.from(Instant.now()));
    dataAccessRequestDAO.insertDARDatasetRelation(
        darToStore.getReferenceId(), dataset.getDatasetId());
    dataAccessRequestDAO.updateRequiresSOApproval(true, darToStore.getReferenceId());
    dataAccessRequestDAO.updateDarApprovalSO(
        signingOfficial.getUserId(), darToStore.getReferenceId());

    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(darToStore.getReferenceId());

    assertEquals(signingOfficial.getUserId(), dar.getApprovingSigningOfficialUserId());
    assertNotNull(dar.getApprovingSigningOfficialApprovedDate());
  }

  /**
   * Replace parent implementation of `createDataset()`
   *
   * @return Dataset
   */
  private Dataset createDARDAOTestDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), null);
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(id);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
    return datasetDAO.findDatasetById(id);
  }

  /**
   * Creates a new user, dataset, data access request, and dar collection
   *
   * @return Populated DataAccessRequest
   */
  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId) {
    DataAccessRequestData data = createDataAccessRequestData();
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, userId, now, now, now, data, randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private DataAccessRequest createProgressReport(
      String eraCommonsId, Integer userId, Integer collectionId, Integer parentId) {
    DataAccessRequestData data = createDataAccessRequestData();
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        parentId, collectionId, referenceId, userId, data, eraCommonsId);
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private static DataAccessRequest createDataAccessRequest(
      Integer collectionId, Integer userId, Date submissionDate) {
    String referenceId = UUID.randomUUID().toString();
    Date createDate = new Date();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
    data.setStatus("test");
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        userId,
        createDate,
        submissionDate,
        new Date(),
        data,
        randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private Integer createDarCollection(Integer createUserId) {
    String darCode = randomAlphabetic(20);
    return darCollectionDAO.insertDarCollection(darCode, createUserId, new Date());
  }

  private DarCollection createDarCollection() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + randomInt(1, 10000);
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
            new Date(),
            referenceId,
            datasetId);
    return electionDAO.findElectionById(electionId);
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private Dataset createDataset(Integer userId) {
    Integer datasetId =
        datasetDAO.insertDataset(
            randomAlphabetic(20),
            new Timestamp(System.currentTimeMillis()),
            userId,
            null,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            null);
    return datasetDAO.findDatasetById(datasetId);
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

  private DataAccessRequest createDraftDataAccessRequest() {
    User user = createUser();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        referenceId, user.getUserId(), now, now, data);
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private Vote createFinalVote(Integer userId, Integer electionId) {
    return createVote(userId, electionId, VoteType.FINAL);
  }

  private Vote createVote(Integer userId, Integer electionId, VoteType voteType) {
    Integer voteId = voteDAO.insertVote(userId, electionId, voteType.getValue());
    return voteDAO.findVoteById(voteId);
  }

  private Election createDataAccessElection(String referenceId, Integer datasetId) {
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            new Date(),
            referenceId,
            datasetId);
    return electionDAO.findElectionById(electionId);
  }
}
