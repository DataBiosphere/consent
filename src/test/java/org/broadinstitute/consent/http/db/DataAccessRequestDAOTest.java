package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.User;
import org.junit.Test;

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

        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAllDraftsByUserId(dar.getData().getUserId());
        assertFalse(newDars.isEmpty());
        assertEquals(1, newDars.size());

        List<DataAccessRequest> missingDars = dataAccessRequestDAO.findAllDraftsByUserId(RandomUtils.nextInt(1, 100));
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

}
