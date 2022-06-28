package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        assertEquals(collection.getDars().size(), newDars.size());
    }

    @Test
    public void testFindAllDrafts() {
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertTrue(dars.isEmpty());

        createDataAccessRequestV3();
        createDraftDataAccessRequest();
        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertFalse(newDars.isEmpty());
        assertEquals(1, newDars.size());
        assertTrue(newDars.get(0).getDraft());
    }

    @Test
    public void testFindAllDraftsByUserId() {
        DataAccessRequest dar = createDraftDataAccessRequest();

        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDraftsByUserId(dar.getUserId());
        assertFalse(newDars.isEmpty());
        assertEquals(1, newDars.size());

        List<DataAccessRequest> missingDars = dataAccessRequestDAO.findAllDraftsByUserId(0);
        assertTrue(missingDars.isEmpty());
    }

    @Test
    public void testFindAllDarsByUserId() {
        DataAccessRequest dar = createDataAccessRequestV3();
        DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(dar.getCollectionId());

        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDarsByUserId(dar.getUserId());
        assertFalse(newDars.isEmpty());
        assertEquals(collection.getDars().size(), newDars.size());
        assertEquals(newDars.get(0).getReferenceId(), dar.getReferenceId());

        List<DataAccessRequest> missingDars = dataAccessRequestDAO.findAllDarsByUserId(0);
        assertTrue(missingDars.isEmpty());
    }
    @Test
    public void updateDraftToNonDraft() {
        DataAccessRequest dar = createDraftDataAccessRequest();

        List<DataAccessRequest> draftDars1 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertFalse(draftDars1.isEmpty());
        assertEquals(1, draftDars1.size());

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
        assertEquals(1, draftDars2.size());
    }

    @Test
    public void testCreate() {
        DataAccessRequest dar = createDataAccessRequestV3();
        DataAccessRequest foundDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        assertNotNull(foundDar);
        assertNotNull(foundDar.getData());
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
        assertEquals(3, dars.size());
    }

    @Test
    public void testUpdateByReferenceId() {
        DataAccessRequest dar = createDataAccessRequestV3();
        String rus = RandomStringUtils.random(10, true, false);
        dar.getData().setRus(rus);
        dar.getData().setValidRestriction(false);
        dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), dar.getData());
        DataAccessRequest updatedDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        assertEquals(rus, updatedDar.getData().getRus());
        assertFalse(updatedDar.getData().getValidRestriction());
    }

    @Test
    public void testUpdateByReferenceIdVersion2() {
        DataAccessRequest dar = createDataAccessRequestV3();
        Date now = new Date();
        User user = createUser();
        String rus = RandomStringUtils.random(10, true, false);
        dar.getData().setRus(rus);
        dar.getData().setValidRestriction(false);
        dataAccessRequestDAO.updateDataByReferenceIdVersion2(dar.getReferenceId(), user.getUserId(), now, now, now, dar.getData());
        DataAccessRequest updatedDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        assertEquals(rus, updatedDar.getData().getRus());
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
    public void  testFindAllDataAccessRequestsForInstitution() {
        //should be included in result
        Integer institutionId = createDataAccessRequestUserWithInstitute();

        //should not be included in result
        createDraftDataAccessRequest();
        createDataAccessRequestV3();

        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDataAccessRequestsForInstitution(institutionId);
        assertFalse(newDars.isEmpty());
        assertEquals(1, newDars.size());

    }

    @Test
    public void testDeleteByCollectionId() {
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
    public void testCancelDeleteByCollectionIds() {
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
    public void testUpdateDraftForCollection() {
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
    public void testArchiveByReferenceIdsStatusChange() {
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
        // add data datasetId
        contents.setDatasetIds(List.of(dataset.getDataSetId()));
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
        return testDar;
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
        Dataset dataset = createDataset();
        User user = createUserWithInstitution();
        DataAccessRequest testDar = createDAR(user, dataset, darCode);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
        List returnedDARs = dataAccessRequestDAO.findAllDataAccessRequests();
        assertTrue(returnedDARs.isEmpty());
    }


    // findAllDataAccessRequests should exclude archived DARs
    // test case with two DARs
    @Test
    public void testFindAllFilterArchived() {
        User user = createUserWithInstitution();

        String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
        String darCode2 = "DAR-" + RandomUtils.nextInt(201, 300);
        Dataset dataset1 = createDataset();
        Dataset dataset2 = createDataset();

        DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
        DataAccessRequest testDar2 = createDAR(user, dataset2, darCode2);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
        List returnedDARs = dataAccessRequestDAO.findAllDataAccessRequests();
        assertEquals(1, returnedDARs.size());
    }


    // findAllDataAccessRequestsByDatasetId should exclude archived DARs
    @Test
    public void testFindAllByDatasetIdArchived() {
        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        Dataset dataset = createDataset();
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequestsByDatasetId(dataset.getDataSetId().toString());
        assertTrue(dars.isEmpty());

        User user = createUserWithInstitution();
        DataAccessRequest testDar = createDAR(user, dataset, darCode);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
        List returnedDARs = dataAccessRequestDAO.findAllDataAccessRequestsByDatasetId(dataset.getDataSetId().toString());
        assertTrue(returnedDARs.isEmpty());
    }

    // findAllDraftDataAccessRequests should exclude archived DARs
    @Test
    public void testFindAllDraftsArchived() {
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertTrue(dars.isEmpty());

        User user = createUserWithInstitution();
        DataAccessRequest testDar = createDraftDAR(user);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
        List returnedDARs = dataAccessRequestDAO.findAllDraftDataAccessRequests();
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
        Dataset dataset = createDataset();

        DataAccessRequest testDar = createDAR(user, dataset, darCode);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
        List returnedDARs = dataAccessRequestDAO.findAllDarsByUserId(user.getUserId());
        assertTrue(returnedDARs.isEmpty());
    }


    // findByReferenceId should exclude archived DARs
    @Test
    public void testFindByReferenceIdArchived() {
        String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
        Dataset dataset = createDataset();
        User user = createUserWithInstitution();
        DataAccessRequest testDar = createDAR(user, dataset, darCode);
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar.getReferenceId()));
        DataAccessRequest returnedDAR = dataAccessRequestDAO.findByReferenceId(testDar.getReferenceId());
        assertNull(returnedDAR);
    }

    // findByReferenceIds should exclude archived DARs
    @Test
    public void testFindByReferenceIdsArchived() {
        String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
        String darCode2 = "DAR-" + RandomUtils.nextInt(201, 300);
        Dataset dataset1 = createDataset();
        Dataset dataset2 = createDataset();
        User user = createUserWithInstitution();
        DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
        DataAccessRequest testDar2 = createDAR(user, dataset2, darCode2);

        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar2.getReferenceId()));
        List returnedDAR = dataAccessRequestDAO.findByReferenceIds(List.of(testDar1.getReferenceId(), testDar2.getReferenceId()));
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
        Dataset dataset1 = createDataset();
        Dataset dataset2 = createDataset();
        Dataset dataset3 = createDataset();
        DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
        DataAccessRequest testDar2 = createDAR(user, dataset2, darCode2);
        DataAccessRequest testDar3 = createDAR(user, dataset3, darCode3);

        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar2.getReferenceId()));
        List returnedDAR = dataAccessRequestDAO.findByReferenceIds(List.of(testDar1.getReferenceId(), testDar2.getReferenceId(), testDar3.getReferenceId()));
        assertEquals(1, returnedDAR.size());
    }

    // findAllDataAccessRequestsForInstitution should exclude archived DARs
    @Test
    public void testFindAllDataAccessRequestsForInstitutionArchived() {
        User user = createUserWithInstitution();
        List<DataAccessRequestData> dars = dataAccessRequestDAO.findAllDataAccessRequestDatas();
        assertTrue(dars.isEmpty());

        String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
        String darCode2 = "DAR-" + RandomUtils.nextInt(201, 300);
        String darCode3 = "DAR-" + RandomUtils.nextInt(301, 400);
        Dataset dataset1 = createDataset();
        Dataset dataset2 = createDataset();
        Dataset dataset3 = createDataset();
        DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
        DataAccessRequest testDar2 = createDAR(user, dataset2, darCode2);
        DataAccessRequest testDar3 = createDAR(user, dataset3, darCode3);

        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar1.getReferenceId()));
        dataAccessRequestDAO.archiveByReferenceIds(List.of(testDar2.getReferenceId()));
        List returnedDAR = dataAccessRequestDAO.findAllDataAccessRequestsForInstitution(user.getInstitutionId());
        assertEquals(1, returnedDAR.size());
    }

    @Test
    public void testFindDARDatasetRelations() {
        String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
        Dataset dataset1 = createDatasetLocal();
        User user = createUserWithInstitution();
        DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
        List<Integer> dataSetIds = dataAccessRequestDAO.findDARDatasetRelations(testDar1.getReferenceId());

        assertNotNull(dataSetIds);
        assertFalse(dataSetIds.isEmpty());
        assertEquals(dataSetIds, testDar1.getData().getDatasetIds());
    }

    @Test
    public void testDeleteDARDatasetRelation() {
        String darCode1 = "DAR-" + RandomUtils.nextInt(100, 200);
        Dataset dataset1 = createDatasetLocal();
        User user = createUserWithInstitution();
        DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
        List<Integer> returned = dataAccessRequestDAO.findDARDatasetRelations(testDar1.getReferenceId());

        assertNotNull(returned);
        assertEquals(testDar1.getData().getDatasetIds(), returned);

        dataAccessRequestDAO.deleteDARDatasetRelationByReferenceId(testDar1.getReferenceId());
        List<Integer> returnedAfter = dataAccessRequestDAO.findDARDatasetRelations(testDar1.getReferenceId());
        assertEquals(returnedAfter, List.of());
    }

    @Test
    public void testMultiDeleteDARDatasetRelation() {
        String darCode1 = "DAR1-" + RandomUtils.nextInt(100, 200);
        String darCode2 = "DAR2-" + RandomUtils.nextInt(100, 200);
        Dataset dataset1 = createDatasetLocal();
        Dataset dataset2 = createDatasetLocal();
        User user = createUserWithInstitution();

        DataAccessRequest testDar1 = createDAR(user, dataset1, darCode1);
        DataAccessRequest testDar2 = createDAR(user, dataset2, darCode2);

        List<Integer> returnedDarDatasets1 = dataAccessRequestDAO.findDARDatasetRelations(testDar1.getReferenceId());
        List<Integer> returnedDarDatasets2 = dataAccessRequestDAO.findDARDatasetRelations(testDar2.getReferenceId());

        assertNotNull(returnedDarDatasets1);
        assertNotNull(returnedDarDatasets2);

        assertEquals(testDar1.getData().getDatasetIds(), returnedDarDatasets1);
        assertEquals(testDar2.getData().getDatasetIds(), returnedDarDatasets2);

        dataAccessRequestDAO.deleteDARDatasetRelationByReferenceIds(List.of(testDar1.getReferenceId(), testDar2.getReferenceId()));

        List<Integer> returnedAfter1 = dataAccessRequestDAO.findDARDatasetRelations(testDar1.getReferenceId());
        List<Integer> returnedAfter2 = dataAccessRequestDAO.findDARDatasetRelations(testDar2.getReferenceId());

        assertEquals(returnedAfter1, List.of());
        assertEquals(returnedAfter2, List.of());
    }

    private Dataset createDatasetLocal() {
        User user = createUser();
        String name = "Name_" + RandomStringUtils.random(20, true, true);
        Timestamp now = new Timestamp(new Date().getTime());
        String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
        Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, true);
        createDatasetPropertiesLocal(id);
        return datasetDAO.findDatasetById(id);
    }

    protected void createDatasetPropertiesLocal(Integer datasetId) {
        List<DatasetProperty> list = new ArrayList<>();
        DatasetProperty dsp = new DatasetProperty();
        dsp.setDataSetId(datasetId);
        dsp.setPropertyKey(1);
        dsp.setPropertyValue("Test_PropertyValue");
        dsp.setCreateDate(new Date());
        list.add(dsp);
        datasetDAO.insertDatasetProperties(list);
    }
}
