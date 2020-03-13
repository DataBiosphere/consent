package org.broadinstitute.consent.http.db;

import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.io.FileUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class DataAccessRequestDAOTest extends DAOTestHelper {

    @Test
    public void testParse() throws Exception {
        String darData =getSampleDarData();
        DataAccessRequestData data = DataAccessRequestData.fromString(darData);
        assertNotNull(data);
        assertNotNull(data.getDarCode());
        assertNotNull(data.getResearcher());
    }

    @Test
    public void testCreate() throws Exception {
        String darData = getSampleDarData();
        DataAccessRequestData data = DataAccessRequestData.fromString(darData);
        String referenceId = UUID.randomUUID().toString();
        dataAccessRequestDAO.insert(referenceId, data);
        DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
        assertNotNull(dar);
        assertNotNull(dar.getData());
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

    private String getSampleDarData() throws Exception {
        return FileUtils.readFileToString(
                new File(ResourceHelpers.resourceFilePath("dataset/dar.json")),
                Charset.defaultCharset());

    }
}
