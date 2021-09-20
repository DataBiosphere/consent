package org.broadinstitute.consent.http.db;

import static junit.framework.TestCase.assertNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DataAccessRequestDAOTest extends DAOTestHelper {

    @Test
    public void testFindAll() {
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDataAccessRequests();
        assertTrue(dars.isEmpty());

        createDataAccessRequestV2();
        createDraftDataAccessRequest();
        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDataAccessRequests();
        assertFalse(newDars.isEmpty());
        assertEquals(1, newDars.size());
    }

    @Test
    public void testFindAllDrafts() {
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertTrue(dars.isEmpty());

        createDataAccessRequestV2();
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
        DataAccessRequest dar = createDataAccessRequestV2();

        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDarsByUserId(dar.getUserId());
        assertFalse(newDars.isEmpty());
        assertEquals(1, newDars.size());
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
        DataAccessRequest dar = createDataAccessRequestV2();

        List<DataAccessRequest> draftDars1 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertTrue(draftDars1.isEmpty());

        dataAccessRequestDAO.updateDraftByReferenceId(dar.referenceId, true);
        List<DataAccessRequest> draftDars2 = dataAccessRequestDAO.findAllDraftDataAccessRequests();
        assertFalse(draftDars2.isEmpty());
        assertEquals(1, draftDars2.size());
    }

    @Test
    public void testCreate() {
        DataAccessRequest dar = createDataAccessRequestV2();
        DataAccessRequest foundDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        assertNotNull(foundDar);
        assertNotNull(foundDar.getData());
    }

    @Test
    public void testFindByReferenceIds() {
        DataAccessRequest dar1 = createDataAccessRequestV2();
        DataAccessRequest dar2 = createDataAccessRequestV2();
        DataAccessRequest dar3 = createDataAccessRequestV2();
        List<String> referenceIds = Arrays.asList(dar1.getReferenceId(), dar2.getReferenceId(), dar3.getReferenceId());

        List<DataAccessRequest> dars = dataAccessRequestDAO.findByReferenceIds(referenceIds);
        assertNotNull(dars);
        assertFalse(dars.isEmpty());
        assertEquals(3, dars.size());
    }

    @Test
    public void testUpdateByReferenceId() {
        DataAccessRequest dar = createDataAccessRequestV2();
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
        DataAccessRequest dar = createDataAccessRequestV2();
        Date now = new Date();
        User user = createUser();
        String rus = RandomStringUtils.random(10, true, false);
        dar.getData().setRus(rus);
        dar.getData().setValidRestriction(false);
        dataAccessRequestDAO.updateDataByReferenceIdVersion2(dar.getReferenceId(), user.getDacUserId(), now, now, now, dar.getData());
        DataAccessRequest updatedDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        assertEquals(rus, updatedDar.getData().getRus());
        assertFalse(updatedDar.getData().getValidRestriction());
    }

    @Test
    public void testInsertVersion2() {
        DataAccessRequest dar = createDataAccessRequestV2();
        assertNotNull(dar);
    }

    @Test
    public void testInsertVersion3() {
        DataAccessRequest dar = createDataAccessRequestV3();
        assertNotNull(dar);
    }

    @Test
    public void testEscapedCharacters() {
        DataAccessRequest dar = createDataAccessRequestV2();
        DataAccessRequest foundDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        assertNotNull(foundDar);
        assertNotNull(foundDar.getData());

        // Tests that "\\\\u0026" in sample dar's projectTitle is converted to "&"
        assertTrue(foundDar.getData().getProjectTitle().contains("&"));
        // Tests that "\\\\u003c" in sample dar's translatedUseRestriction is converted to "<"
        assertTrue(foundDar.getData().getTranslatedUseRestriction().contains("<"));
        // Tests that "\\\\u003e" in sample dar's translatedUseRestriction is converted to ">"
        assertTrue(foundDar.getData().getTranslatedUseRestriction().contains(">"));
    }

    @Test
    public void  testFindAllDataAccessRequestsForInstitution() {
        //should be included in result
        Integer institutionId = createDataAccessRequestUserWithInstitute();

        //should not be included in result
        createDraftDataAccessRequest();
        createDataAccessRequestV2();

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

        assertNotNull(updatedDar1.getData().getAddress1());
        assertNotNull(updatedDar2.getData().getAddress1());
        assertEquals(dar1.getData().getAddress1(), updatedDar1.getData().getAddress1());
        assertEquals(dar2.getData().getAddress1(), updatedDar2.getData().getAddress1());
    }

}
