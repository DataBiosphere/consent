package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetEntry;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Test;

class DataAccessRequestDAOTest extends DAOTestHelper {

  @Test
  void testFindAll() {
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequests();
    assertTrue(dars.isEmpty());

    DataAccessRequest collectionDar = createDataAccessRequestV3();
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(
        collectionDar.getCollectionId());
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
    dataAccessRequestDAO.insertDARDatasetRelation(draft.getReferenceId(), d1.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(draft.getReferenceId(), d2.getDataSetId());
    List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertFalse(newDars.isEmpty());
    assertEquals(1, newDars.size());
    assertTrue(newDars.get(0).getDraft());
  }

  @Test
  void testFindAllDraftsByUserId() {
    DataAccessRequest dar = createDraftDataAccessRequest();
    Dataset d1 = createDARDAOTestDataset();
    Dataset d2 = createDARDAOTestDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDataSetId());

    List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDraftsByUserId(dar.getUserId());
    assertFalse(newDars.isEmpty());
    assertEquals(1, newDars.size());

    List<DataAccessRequest> missingDars = dataAccessRequestDAO.findAllDraftsByUserId(0);
    assertTrue(missingDars.isEmpty());
  }

  @Test
  void testFindAllDarsByUserId() {
    DataAccessRequest dar = createDataAccessRequestV3();
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(
        dar.getCollectionId());

    List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDarsByUserId(dar.getUserId());
    assertFalse(newDars.isEmpty());
    assertEquals(collection.getDars().size(), newDars.size());
    assertEquals(newDars.get(0).getReferenceId(), dar.getReferenceId());

    List<DataAccessRequest> missingDars = dataAccessRequestDAO.findAllDarsByUserId(0);
    assertTrue(missingDars.isEmpty());
  }

  @Test
  void updateDraftToNonDraft() {
    DataAccessRequest dar = createDraftDataAccessRequest();

    List<DataAccessRequest> draftDars1 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertFalse(draftDars1.isEmpty());
    assertEquals(1, draftDars1.size());

    dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, false);
    List<DataAccessRequest> draftDars2 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertTrue(draftDars2.isEmpty());
  }

  @Test
  void updateNonDraftToDraft() {
    DataAccessRequest dar = createDataAccessRequestV3();

    List<DataAccessRequest> draftDars1 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertTrue(draftDars1.isEmpty());

    dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, true);
    List<DataAccessRequest> draftDars2 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
    assertFalse(draftDars2.isEmpty());
    assertEquals(1, draftDars2.size());
  }


  @Test
  void updateDraftToNonDraftByCollectionId() {
    DarCollection darColl = createDarCollection();
    DataAccessRequest dar = new ArrayList<>(darColl.getDars().values()).get(0);

    dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, true);
    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertEquals(true, dar.getDraft());
    dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, false);
    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertEquals(false, dar.getDraft());
  }

  @Test
  void updateNonDraftToDraftByCollectionId() {
    DarCollection darColl = createDarCollection();
    DataAccessRequest dar = new ArrayList<>(darColl.getDars().values()).get(0);

    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertEquals(false, dar.getDraft());
    dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, true);
    dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertEquals(true, dar.getDraft());
  }

  @Test
  void testCreate() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + RandomUtils.nextInt(1, 999999999);
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    Dataset d1 = createDARDAOTestDataset();
    Dataset d2 = createDARDAOTestDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDataSetId());
    DataAccessRequest foundDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
    assertNotNull(foundDar);
    assertNotNull(foundDar.getData());
    assertTrue(foundDar.getDatasetIds().contains(d1.getDataSetId()));
    assertTrue(foundDar.getDatasetIds().contains(d2.getDataSetId()));
  }

  @Test
  void testFindByReferenceIds() {
    DataAccessRequest dar1 = createDataAccessRequestV3();
    DataAccessRequest dar2 = createDataAccessRequestV3();
    DataAccessRequest dar3 = createDataAccessRequestV3();
    List<String> referenceIds = Arrays.asList(dar1.getReferenceId(), dar2.getReferenceId(),
        dar3.getReferenceId());

    List<DataAccessRequest> dars = dataAccessRequestDAO.findByReferenceIds(referenceIds);
    assertNotNull(dars);
    assertFalse(dars.isEmpty());
    assertEquals(3, dars.size());
  }

  @Test
  void testUpdateByReferenceId() {
    DataAccessRequest dar = createDataAccessRequestV3();
    Date now = new Date();
    User user = createUser();
    String rus = RandomStringUtils.random(10, true, false);
    dar.getData().setRus(rus);
    dar.getData().setValidRestriction(false);
    dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), user.getUserId(), now, now,
        now, dar.getData());
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
        now, now, now, now,
        data);
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

    String rus = RandomStringUtils.random(10, true, false);
    dar.getData().setRus(rus + String.format(" %s ", unsupportedUnicode));
    dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), collection.getCreateUserId(),
        now, now,
        now, dar.getData());

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
        referenceId,
        user.getUserId(),
        now,
        now,
        now,
        now,
        data
    );
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
    //creates a dar with a collection ID (also creates a DarCollection)
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
  void testUpdateDraftForCollection() {
    DarCollection collection = createDarCollection();
    DataAccessRequest draft = createDraftDataAccessRequest();
    String referenceId = draft.getReferenceId();
    Integer collectionId = collection.getDarCollectionId();
    dataAccessRequestDAO.updateDraftForCollection(collectionId, referenceId);
    DataAccessRequest updatedDraft = dataAccessRequestDAO.findByReferenceId(referenceId);
    assertEquals(false, updatedDraft.getDraft());
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

  // local method to create a DAR
  protected DataAccessRequest createDAR(User user, Dataset dataset, String darCode) {
    Timestamp now = new Timestamp(new Date().getTime());
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    DataAccessRequest testDar = new DataAccessRequest();
    testDar.setCollectionId(collectionId);
    testDar.setReferenceId(UUID.randomUUID().toString());
    testDar.setUserId(user.getUserId());
    testDar.setCreateDate(now);
    testDar.setSortDate(now);
    testDar.setSubmissionDate(now);
    testDar.setUpdateDate(now);
    DataAccessRequestData contents = new DataAccessRequestData();
    testDar.setData(contents);
    dataAccessRequestDAO.insertDataAccessRequest(
        testDar.getCollectionId(),
        testDar.getReferenceId(),
        testDar.getUserId(),
        testDar.getCreateDate(),
        testDar.getSortDate(),
        testDar.getSubmissionDate(),
        testDar.getUpdateDate(),
        testDar.getData()
    );
    dataAccessRequestDAO.insertDARDatasetRelation(testDar.getReferenceId(), dataset.getDataSetId());
    return dataAccessRequestDAO.findByReferenceId(testDar.getReferenceId());
  }

  // local method to create a Draft DAR
  protected DataAccessRequest createDraftDAR(User user) {
    Date now = new Date();
    DataAccessRequestData contents = new DataAccessRequestData();
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        referenceId,
        user.getUserId(),
        now,
        now,
        now,
        now,
        contents
    );
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  // findAllDataAccessRequests should exclude archived DARs
  @Test
  void testFindAllArchived() {
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequests();
    assertTrue(dars.isEmpty());

    String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
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

    String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
    String darCode2 = "DAR-" + RandomUtils.nextInt(201, 300);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();

    DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
    createDAR(user, dataset2, darCode2);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
    List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllDataAccessRequests();
    assertEquals(1, returnedDARs.size());
  }

  // See: https://broadworkbench.atlassian.net/browse/DUOS-2182
  @Test
  void testEnsureOnlyDataAccessRequestsByDatasetIdReturnsJustForSpecificDatasetId() {
    String darCode1 = "DAR-" + RandomUtils.nextInt(100, 1000);
    String darCode2 = "DAR-" + RandomUtils.nextInt(100, 1000);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();
    User user1 = createUser();
    User user2 = createUser();
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1);
    DataAccessRequest testDar2 = createDAR(user2, dataset2, darCode2);

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDataSetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), e1.getElectionId());
    Date now = new Date();
    voteDAO.updateVote(true,
        "",
        now,
        v1.getVoteId(),
        false,
        e1.getElectionId(),
        now,
        false);

    Election e2 = createDataAccessElection(testDar2.getReferenceId(), dataset2.getDataSetId());
    Vote v2 = createFinalVote(dataset2.getCreateUserId(), e2.getElectionId());
    now = new Date();
    voteDAO.updateVote(true,
        "",
        now,
        v2.getVoteId(),
        false,
        e2.getElectionId(),
        now,
        false);

    List<DataAccessRequest> dars = dataAccessRequestDAO.findApprovedDARsByDatasetId(
        dataset1.getDataSetId());
    assertEquals(1, dars.size());
    assertTrue(dars.get(0).getDatasetIds().contains(dataset1.getDataSetId()));
  }

  @Test
  void testFindAllApprovedDataAccessRequestsByDatasetId() {
    String darCode1 = "DAR-" + RandomUtils.nextInt(100, 1000000);
    String darCode2 = "DAR-" + RandomUtils.nextInt(100, 1000000);
    String darCode3 = "DAR-" + RandomUtils.nextInt(100, 1000000);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();

    assertTrue(
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDataSetId())
            .isEmpty());
    assertTrue(
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDataSetId())
            .isEmpty());

    User user1 = createUserWithInstitution();
    User user2 = createUserWithInstitution();
    User user3 = createUserWithInstitution();
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1);
    DataAccessRequest testDar2 = createDAR(user2, dataset2, darCode2);
    DataAccessRequest testDar3 = createDAR(user3, dataset2, darCode3);
    assertTrue(
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDataSetId())
            .isEmpty());
    assertTrue(
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDataSetId())
            .isEmpty());

    assertEquals(0,
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDataSetId())
            .size());

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDataSetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), e1.getElectionId());
    Date now = new Date();
    voteDAO.updateVote(true,
        "",
        now,
        v1.getVoteId(),
        false,
        e1.getElectionId(),
        now,
        false);

    Election e2 = createDataAccessElection(testDar2.getReferenceId(), dataset2.getDataSetId());
    Vote v2 = createFinalVote(dataset2.getCreateUserId(), e2.getElectionId());
    now = new Date();
    voteDAO.updateVote(true,
        "",
        now,
        v2.getVoteId(),
        false,
        e2.getElectionId(),
        now,
        false);

    assertEquals(1,
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDataSetId())
            .size());
    assertEquals(testDar1.getUserId(),
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDataSetId())
            .get(0).getUserId());
    assertEquals(1,
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDataSetId())
            .size());
    assertEquals(testDar2.getUserId(),
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDataSetId())
            .get(0).getUserId());

    Election e3 = createDataAccessElection(testDar3.getReferenceId(), dataset2.getDataSetId());
    Vote v3 = createFinalVote(dataset2.getCreateUserId(), e3.getElectionId());
    now = new Date();
    voteDAO.updateVote(true,
        "",
        now,
        v3.getVoteId(),
        false,
        e3.getElectionId(),
        now,
        false);

    List<DataAccessRequest> approvedDars = dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset2.getDataSetId());
    List<Integer> approvedDarIds = approvedDars.stream().map(DataAccessRequest::getId).toList();
    assertEquals(2, approvedDarIds.size());
    assertTrue(approvedDarIds.contains(testDar3.getId()));
    assertTrue(approvedDarIds.contains(testDar2.getId()));
    assertFalse(approvedDarIds.contains(testDar1.getId()));
  }

  /**
   * Tests the case where a user has been approved for access, then denied access, and that the user
   * does not show up as an approved user for the dataset.
   */
  @Test
  void testFindAllApprovedDataAccessRequestsByDatasetId_ApprovedThenDeniedCase() {
    String darCode1 = "DAR-" + RandomUtils.nextInt(100, 1000000);
    Dataset dataset1 = createDARDAOTestDataset();
    User user1 = createUserWithInstitution();
    DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1);

    Election e1 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDataSetId());
    Vote v1 = createFinalVote(dataset1.getCreateUserId(), e1.getElectionId());
    Date now = new Date();
    voteDAO.updateVote(true,
        "",
        now,
        v1.getVoteId(),
        false,
        e1.getElectionId(),
        now,
        false);

    assertEquals(1,
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDataSetId())
            .size());

    Election e2 = createDataAccessElection(testDar1.getReferenceId(), dataset1.getDataSetId());
    Vote v2 = createFinalVote(dataset1.getCreateUserId(), e2.getElectionId());
    now = new Date();
    voteDAO.updateVote(false,
        "",
        now,
        v2.getVoteId(),
        false,
        e2.getElectionId(),
        now,
        false);

    assertEquals(0,
        dataAccessRequestDAO.findApprovedDARsByDatasetId(dataset1.getDataSetId())
            .size());
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
    List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllDraftsByUserId(
        user.getUserId());
    assertTrue(returnedDARs.isEmpty());
  }


  // findAllDarsByUserId should exclude archived DARs
  @Test
  void testFindAllDarsByUserIdArchived() {
    User user = createUserWithInstitution();
    List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDarsByUserId(user.getUserId());
    assertTrue(dars.isEmpty());

    String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
    Dataset dataset = createDARDAOTestDataset();

    DataAccessRequest testDar = createDAR(user, dataset, darCode);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
    List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllDarsByUserId(
        user.getUserId());
    assertTrue(returnedDARs.isEmpty());
  }


  // findByReferenceId should exclude archived DARs
  @Test
  void testFindByReferenceIdArchived() {
    String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
    Dataset dataset = createDARDAOTestDataset();
    User user = createUserWithInstitution();
    DataAccessRequest testDar = createDAR(user, dataset, darCode);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
    DataAccessRequest returnedDAR = dataAccessRequestDAO.findByReferenceId(
        testDar.getReferenceId());
    assertNull(returnedDAR);
  }

  // findByReferenceIds should exclude archived DARs
  @Test
  void testFindByReferenceIdsArchived() {
    String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
    String darCode2 = "DAR-" + RandomUtils.nextInt(201, 300);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();
    User user = createUserWithInstitution();
    DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
    DataAccessRequest testDar2 = createDAR(user, dataset2, darCode2);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar2.getReferenceId()));
    List<DataAccessRequest> returnedDAR = dataAccessRequestDAO.findByReferenceIds(
        List.of(testDar1.getReferenceId(), testDar2.getReferenceId()));
    assertTrue(returnedDAR.isEmpty());
  }

  // findAllDataAccessRequestDatas should exclude archived DARs
  @Test
  void testFindAllDataAccessRequestDatasArchived() {
    User user = createUserWithInstitution();
    List<DataAccessRequestData> dars = dataAccessRequestDAO.findAllDataAccessRequestDatas();
    assertTrue(dars.isEmpty());

    String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
    String darCode2 = "DAR-" + RandomUtils.nextInt(201, 300);
    String darCode3 = "DAR-" + RandomUtils.nextInt(301, 400);
    Dataset dataset1 = createDARDAOTestDataset();
    Dataset dataset2 = createDARDAOTestDataset();
    Dataset dataset3 = createDARDAOTestDataset();
    DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
    DataAccessRequest testDar2 = createDAR(user, dataset2, darCode2);
    DataAccessRequest testDar3 = createDAR(user, dataset3, darCode3);

    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
    dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar2.getReferenceId()));
    List<DataAccessRequest> returnedDAR = dataAccessRequestDAO.findByReferenceIds(
        List.of(testDar1.getReferenceId(), testDar2.getReferenceId(), testDar3.getReferenceId()));
    assertEquals(1, returnedDAR.size());
  }

  /**
   * Replace parent implementation of `createDataset()`
   *
   * @return Dataset
   */
  private Dataset createDARDAOTestDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDataSetId(id);
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
  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId,
      String darCode) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + RandomStringUtils.random(50, true, false));
    data.setDarCode(darCode);
    DatasetEntry entry = new DatasetEntry();
    entry.setKey("key");
    entry.setValue("value");
    entry.setLabel("label");
    data.setDatasets(List.of(entry));
    data.setHmb(true);
    data.setMethods(false);
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        userId,
        now, now, now, now,
        data);
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private DarCollection createDarCollection() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + RandomUtils.nextInt(1, 10000);
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
    Election cancelled = createCancelledAccessElection(dar.getReferenceId(),
        dataset.getDataSetId());
    Election access = createDataAccessElection(dar.getReferenceId(), dataset.getDataSetId());
    createFinalVote(user.getUserId(), cancelled.getElectionId());
    createFinalVote(user.getUserId(), access.getElectionId());
    createDataAccessRequest(user.getUserId(), collection_id, darCode);
    createDataAccessRequest(user.getUserId(), collection_id, darCode);
    return darCollectionDAO.findDARCollectionByCollectionId(collection_id);
  }

  private Election createCancelledAccessElection(String referenceId, Integer datasetId) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.CANCELED.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    return electionDAO.findElectionById(electionId);
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDataSetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private DataAccessRequest createDraftDataAccessRequest() {
    User user = createUser();
    String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + RandomStringUtils.random(50, true, false));
    data.setDarCode(darCode);
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        referenceId,
        user.getUserId(),
        now,
        now,
        now,
        now,
        data
    );
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private Vote createFinalVote(Integer userId, Integer electionId) {
    Integer voteId = voteDAO.insertVote(userId, electionId, VoteType.FINAL.getValue());
    return voteDAO.findVoteById(voteId);
  }

  private Election createDataAccessElection(String referenceId, Integer datasetId) {
    Integer electionId = electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        referenceId,
        datasetId
    );
    return electionDAO.findElectionById(electionId);
  }

  private User createUserWithInstitution() {
    int i1 = RandomUtils.nextInt(5, 10);
    String email = RandomStringUtils.randomAlphabetic(i1);
    String name = RandomStringUtils.randomAlphabetic(10);
    Integer userId = userDAO.insertUser(email, name, new Date());
    Integer institutionId = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        userId,
        new Date());
    userDAO.updateUser(name, userId, institutionId);
    userRoleDAO.insertSingleUserRole(7, userId);
    return userDAO.findUserById(userId);
  }

}
