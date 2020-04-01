package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DataAccessRequestDAOTest extends DAOTestHelper {

    @Test
    public void testFindAll() {
        List<DataAccessRequest> dars = dataAccessRequestDAO.findAll();
        assertTrue(dars.isEmpty());

        createDataAccessRequest();
        List<DataAccessRequest> newDars = dataAccessRequestDAO.findAll();
        assertFalse(newDars.isEmpty());
        assertEquals(1, newDars.size());
    }

    @Test
    public void testCreate() {
        DataAccessRequest dar = createDataAccessRequest();
        DataAccessRequest foundDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        assertNotNull(foundDar);
        assertNotNull(foundDar.getData());
    }

    @Test
    public void testFindByReferenceId() {
        // no-op ... tested by createDataAccessRequest()
    }

    @Test
    public void testFindByReferenceIds() {
        DataAccessRequest dar1 = createDataAccessRequest();
        DataAccessRequest dar2 = createDataAccessRequest();
        DataAccessRequest dar3 = createDataAccessRequest();
        List<String> referenceIds = Arrays.asList(dar1.getReferenceId(), dar2.getReferenceId(), dar3.getReferenceId());

        List<DataAccessRequest> dars = dataAccessRequestDAO.findByReferenceIds(referenceIds);
        assertNotNull(dars);
        assertFalse(dars.isEmpty());
        assertEquals(3, dars.size());
    }

    @Test
    public void testDeleteByReferenceId() {
        // no-op ... tested by tearDown()
    }

    @Test
    public void testUpdateByReferenceId() {
        DataAccessRequest dar = createDataAccessRequest();
        String rus = RandomStringUtils.random(10, true, false);
        dar.getData().setRus(rus);
        dar.getData().setValidRestriction(false);
        dataAccessRequestDAO.updateDataByReferenceId(dar.getReferenceId(), dar.getData());
        DataAccessRequest updatedDar = dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
        assertEquals(rus, updatedDar.getData().getRus());
        assertFalse(updatedDar.getData().getValidRestriction());
    }

    @Test
    public void testEscapedCharacters() {
        DataAccessRequest dar = createDataAccessRequest();
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
