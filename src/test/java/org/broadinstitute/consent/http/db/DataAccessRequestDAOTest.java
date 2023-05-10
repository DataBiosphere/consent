package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DataAccessRequestDAOTest extends DAOTestHelper {

    @Test
    public void testFindAll() {
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequests();
        assertTrue(dars.isEmpty());

        DataAccessRequest collectionDar = createDataAccessRequestV3();
        DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionDar.getCollectionId());
        createDraftDataAccessRequest();
        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDataAccessRequests();
        assertFalse(newDars.isEmpty());
        Assertions.assertEquals(collection.getDars().size(), newDars.size());
    }

    @Test
    public void testFindAllDrafts() {
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
        Assertions.assertEquals(1, newDars.size());
        assertTrue(newDars.get(0).getDraft());
    }

    @Test
    public void testFindAllDraftsByUserId() {
        DataAccessRequest dar = createDraftDataAccessRequest();
        Dataset d1 = createDARDAOTestDataset();
        Dataset d2 = createDARDAOTestDataset();
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d1.getDataSetId());
        dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), d2.getDataSetId());

        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDraftsByUserId(dar.getUserId());
        assertFalse(newDars.isEmpty());
        Assertions.assertEquals(1, newDars.size());

        List<DataAccessRequest> missingDars = dataAccessRequestDAO.findAllDraftsByUserId(0);
        assertTrue(missingDars.isEmpty());
    }

    @Test
    public void testFindAllDarsByUserId() {
        DataAccessRequest dar = createDataAccessRequestV3();
        DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(dar.getCollectionId());

        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDarsByUserId(dar.getUserId());
        assertFalse(newDars.isEmpty());
        Assertions.assertEquals(collection.getDars().size(), newDars.size());
        Assertions.assertEquals(newDars.get(0).getReferenceId(), dar.getReferenceId());

        List<DataAccessRequest> missingDars = dataAccessRequestDAO.findAllDarsByUserId(0);
        assertTrue(missingDars.isEmpty());
    }

    @Test
    public void updateDraftToNonDraft() {
        DataAccessRequest dar = createDraftDataAccessRequest();

        List<DataAccessRequest> draftDars1 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertFalse(draftDars1.isEmpty());
        Assertions.assertEquals(1, draftDars1.size());

        dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, false);
        List<DataAccessRequest> draftDars2 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertTrue(draftDars2.isEmpty());
    }

    @Test
    public void updateNonDraftToDraft() {
        DataAccessRequest dar = createDataAccessRequestV3();

        List<DataAccessRequest> draftDars1 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertTrue(draftDars1.isEmpty());

        dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, true);
        List<DataAccessRequest> draftDars2 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertFalse(draftDars2.isEmpty());
        Assertions.assertEquals(1, draftDars2.size());
    }


    @Test
    public void updateDraftToNonDraftByCollectionId() {
        DarCollection darColl = createDarCollection();
        DataAccessRequest dar = new ArrayList<>(darColl.getDars().values()).get(0);

        dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, true);
        dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        Assertions.assertEquals(true, dar.getDraft());
        dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, false);
        dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        Assertions.assertEquals(false, dar.getDraft());
    }

    @Test
    public void updateNonDraftToDraftByCollectionId() {
        DarCollection darColl = createDarCollection();
        DataAccessRequest dar = new ArrayList<>(darColl.getDars().values()).get(0);

        dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        Assertions.assertEquals(false, dar.getDraft());
        dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, true);
        dar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        Assertions.assertEquals(true, dar.getDraft());
    }

    @Test
    public void testCreate() {
        User user = createUserWithInstitution();
        String darCode = "DAR-" + RandomUtils.nextInt(1, 999999999);
        Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
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
    public void testFindByReferenceIds() {
        DataAccessRequest dar1 = createDataAccessRequestV3();
        DataAccessRequest dar2 = createDataAccessRequestV3();
        DataAccessRequest dar3 = createDataAccessRequestV3();
        List<String> referenceIds = Arrays.asList(dar1.getReferenceId(), dar2.getReferenceId(), dar3.getReferenceId());

        List<DataAccessRequest> dars = dataAccessRequestDAO.findByReferenceIds(referenceIds);
        assertNotNull(dars);
        assertFalse(dars.isEmpty());
        Assertions.assertEquals(3, dars.size());
    }

    @Test
    public void testUpdateByReferenceId() {
        DataAccessRequest dar = createDataAccessRequestV3();
        Date now = new Date();
        User user = createUser();
        String rus = RandomStringUtils.random(10, true, false);
        dar.getData().setRus(rus);
        dar.getData().setValidRestriction(false);
        dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), user.getUserId(), now, now, now, dar.getData());
        DataAccessRequest updatedDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        Assertions.assertEquals(rus, updatedDar.getData().getRus());
        assertFalse(updatedDar.getData().getValidRestriction());
    }

    @Test
    public void testInsertDraftDataAccessRequest() {
        DataAccessRequest dar = createDraftDataAccessRequest();
        assertNotNull(dar);
    }

    @Test
    public void testInsertVersion3() {
        DataAccessRequest dar = createDataAccessRequestV3();
        assertNotNull(dar);
    }

    @Test
    public void testDeleteByCollectionId() {
        //creates a dar with a collection ID (also creates a DarCollection)
        DataAccessRequest dar = createDataAccessRequestV3();
        DataAccessRequest returned = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        assertNotNull(returned);
        Assertions.assertEquals(dar.getId(), returned.getId());
        dataAccessRequestDAO.deleteByCollectionId(dar.getCollectionId());
        DataAccessRequest returnedAfter = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        Assertions.assertNull(returnedAfter);

    }

    @Test
    public void testCancelDeleteByCollectionIds() {
        DataAccessRequest dar1 = createDataAccessRequestV3();
        DataAccessRequest dar2 = createDataAccessRequestV3();

        List<String> referenceIds = new ArrayList<>();
        referenceIds.add(dar1.getReferenceId());
        referenceIds.add(dar2.getReferenceId());

        dataAccessRequestDAO.cancelByReferenceIds(referenceIds);

        DataAccessRequest updatedDar1 = dataAccessRequestDAO.findByReferenceId(dar1.getReferenceId());
        DataAccessRequest updatedDar2 = dataAccessRequestDAO.findByReferenceId(dar2.getReferenceId());

        Assertions.assertEquals(dar1.getReferenceId(), updatedDar1.getReferenceId());
        Assertions.assertEquals(dar2.getReferenceId(), updatedDar2.getReferenceId());

        Assertions.assertEquals("Canceled", updatedDar1.getData().getStatus());
        Assertions.assertEquals("Canceled", updatedDar2.getData().getStatus());

        assertNotNull(updatedDar1.getData().getHmb());
        assertNotNull(updatedDar2.getData().getHmb());
        Assertions.assertEquals(dar1.getData().getHmb(), updatedDar1.getData().getHmb());
        Assertions.assertEquals(dar2.getData().getHmb(), updatedDar2.getData().getHmb());

        assertNotNull(updatedDar1.getData().getMethods());
        assertNotNull(updatedDar2.getData().getMethods());
        Assertions.assertEquals(dar1.getData().getMethods(), updatedDar1.getData().getMethods());
        Assertions.assertEquals(dar2.getData().getMethods(), updatedDar2.getData().getMethods());
    }

    @Test
    public void testUpdateDraftForCollection() {
        DarCollection collection = createDarCollection();
        DataAccessRequest draft = createDraftDataAccessRequest();
        String referenceId = draft.getReferenceId();
        Integer collectionId = collection.getDarCollectionId();
        dataAccessRequestDAO.updateDraftForCollection(collectionId, referenceId);
        DataAccessRequest updatedDraft = dataAccessRequestDAO.findByReferenceId(referenceId);
        Assertions.assertEquals(false, updatedDraft.getDraft());
        Assertions.assertEquals(collectionId, updatedDraft.getCollectionId());
    }

    @Test
    public void testArchiveByReferenceIdsStatusChange() {
        DataAccessRequest dar = createDataAccessRequestV3();
        List<String> referenceIds = List.of(dar.getReferenceId());
        dataAccessRequestDAO.cancelByReferenceIds(referenceIds);
        DataAccessRequest canceledDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());

        Assertions.assertEquals(dar.getReferenceId(), canceledDar.getReferenceId());
        Assertions.assertEquals("Canceled", canceledDar.getData().getStatus());
        assertNotNull(canceledDar.getData().getHmb());
        Assertions.assertEquals(dar.getData().getHmb(), canceledDar.getData().getHmb());
        assertNotNull(canceledDar.getData().getMethods());
        Assertions.assertEquals(dar.getData().getMethods(), canceledDar.getData().getMethods());
    }

    // local method to create a DAR
    protected DataAccessRequest createDAR(User user, Dataset dataset, String darCode) {
        Timestamp now = new Timestamp(new Date().getTime());
        Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
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
    public void testFindAllArchived() {
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
    public void testFindAllFilterArchived() {
        User user = createUserWithInstitution();

        String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
        String darCode2 = "DAR-" + RandomUtils.nextInt(201, 300);
        Dataset dataset1 = createDARDAOTestDataset();
        Dataset dataset2 = createDARDAOTestDataset();

        DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
        createDAR(user, dataset2, darCode2);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
        List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllDataAccessRequests();
        Assertions.assertEquals(1, returnedDARs.size());
    }


    // findAllDataAccessRequestsByDatasetId should exclude archived DARs
    @Test
    public void testFindAllByDatasetIdArchived() {
        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        Dataset dataset = createDARDAOTestDataset();
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllApprovedDataAccessRequestsByDatasetId(dataset.getDataSetId());
        assertTrue(dars.isEmpty());

        User user = createUserWithInstitution();
        DataAccessRequest testDar = createDAR(user, dataset, darCode);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
        List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllApprovedDataAccessRequestsByDatasetId(dataset.getDataSetId());
        assertTrue(returnedDARs.isEmpty());
    }

    // See: https://broadworkbench.atlassian.net/browse/DUOS-2182
    @Test
    public void testEnsureOnlyDataAccessRequestsByDatasetIdReturnsJustForSpecificDatasetId() {
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

        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllApprovedDataAccessRequestsByDatasetId(dataset1.getDataSetId());
        Assertions.assertEquals(1, dars.size());
        assertTrue(dars.get(0).datasetIds.contains(dataset1.getDataSetId()));
    }

    @Test
    public void testFindAllApprovedDataAccessRequestsByDatasetId() {
        String darCode1 = "DAR-" + RandomUtils.nextInt(100, 1000000);
        String darCode2 = "DAR-" + RandomUtils.nextInt(100, 1000000);
        String darCode3 = "DAR-" + RandomUtils.nextInt(100, 1000000);
        Dataset dataset1 = createDARDAOTestDataset();
        Dataset dataset2 = createDARDAOTestDataset();

        assertTrue(
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset1.getDataSetId()).isEmpty());
        assertTrue(
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset2.getDataSetId()).isEmpty());

        User user1 = createUserWithInstitution();
        User user2 = createUserWithInstitution();
        User user3 = createUserWithInstitution();
        DataAccessRequest testDar1 = createDAR(user1, dataset1, darCode1);
        DataAccessRequest testDar2 = createDAR(user2, dataset2, darCode2);
        DataAccessRequest testDar3 = createDAR(user3, dataset2, darCode3);
        assertTrue(
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset1.getDataSetId()).isEmpty());
        assertTrue(
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset2.getDataSetId()).isEmpty());

        Assertions.assertEquals(0, dataAccessRequestDAO.findAllApprovedDataAccessRequestsByDatasetId(dataset2.getDataSetId()).size());

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

        Assertions.assertEquals(1,
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset1.getDataSetId()).size());
        Assertions.assertEquals(testDar1.getUserId(),
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset1.getDataSetId()).get(0));
        Assertions.assertEquals(1,
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset2.getDataSetId()).size());
        Assertions.assertEquals(testDar2.getUserId(),
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset2.getDataSetId()).get(0));

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

        Assertions.assertEquals(2,
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset2.getDataSetId()).size());
        assertTrue(
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset2.getDataSetId()).contains(testDar3.getUserId()));
        assertTrue(
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset2.getDataSetId()).contains(testDar2.getUserId()));
        assertFalse(
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset2.getDataSetId()).contains(testDar1.getUserId()));
    }

    /**
     * Tests the case where a user has been approved for access, then denied access,
     * and that the user does not show up as an approved user for the dataset.
     */
    @Test
    public void testFindAllApprovedDataAccessRequestsByDatasetId_ApprovedThenDeniedCase() {
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

        Assertions.assertEquals(1,
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset1.getDataSetId()).size());

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

        Assertions.assertEquals(0,
            dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(dataset1.getDataSetId()).size());
    }

    // findAllDraftDataAccessRequests should exclude archived DARs
    @Test
    public void testFindAllDraftsArchived() {
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
    public void testFindAllDraftsByUserIdArchived() {
        User user = createUserWithInstitution();

        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDraftsByUserId(user.getUserId());
        assertTrue(dars.isEmpty());

        DataAccessRequest testDar = createDraftDAR(user);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
        List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllDraftsByUserId(user.getUserId());
        assertTrue(returnedDARs.isEmpty());
    }


    // findAllDarsByUserId should exclude archived DARs
    @Test
    public void testFindAllDarsByUserIdArchived() {
        User user = createUserWithInstitution();
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDarsByUserId(user.getUserId());
        assertTrue(dars.isEmpty());

        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        Dataset dataset = createDARDAOTestDataset();

        DataAccessRequest testDar = createDAR(user, dataset, darCode);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
        List<DataAccessRequest> returnedDARs = dataAccessRequestDAO.findAllDarsByUserId(user.getUserId());
        assertTrue(returnedDARs.isEmpty());
    }


    // findByReferenceId should exclude archived DARs
    @Test
    public void testFindByReferenceIdArchived() {
        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        Dataset dataset = createDARDAOTestDataset();
        User user = createUserWithInstitution();
        DataAccessRequest testDar = createDAR(user, dataset, darCode);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
        DataAccessRequest returnedDAR = dataAccessRequestDAO.findByReferenceId(testDar.getReferenceId());
        Assertions.assertNull(returnedDAR);
    }

    // findByReferenceIds should exclude archived DARs
    @Test
    public void testFindByReferenceIdsArchived() {
        String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
        String darCode2 = "DAR-" + RandomUtils.nextInt(201, 300);
        Dataset dataset1 = createDARDAOTestDataset();
        Dataset dataset2 = createDARDAOTestDataset();
        User user = createUserWithInstitution();
        DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
        DataAccessRequest testDar2 = createDAR(user, dataset2, darCode2);

        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar2.getReferenceId()));
        List<DataAccessRequest> returnedDAR = dataAccessRequestDAO.findByReferenceIds(List.of(testDar1.getReferenceId(), testDar2.getReferenceId()));
        assertTrue(returnedDAR.isEmpty());
    }

    // findAllDataAccessRequestDatas should exclude archived DARs
    @Test
    public void testFindAllDataAccessRequestDatasArchived() {
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
        List<DataAccessRequest> returnedDAR = dataAccessRequestDAO.findByReferenceIds(List.of(testDar1.getReferenceId(), testDar2.getReferenceId(), testDar3.getReferenceId()));
        Assertions.assertEquals(1, returnedDAR.size());
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
        Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, false, dataUse.toString(), null);
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

}
