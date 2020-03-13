package org.broadinstitute.consent.http.db;

import io.dropwizard.testing.ResourceHelpers;
import org.apache.commons.io.FileUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.util.UUID;

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
        String referenceId = UUID.randomUUID().toString();
        dataAccessRequestDAO.insert(referenceId, darData);
        DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
        assertNotNull(dar);
        assertNotNull(dar.getData());
    }

    private String getSampleDarData() throws Exception {
        return FileUtils.readFileToString(
                new File(ResourceHelpers.resourceFilePath("dataset/dar.json")),
                Charset.defaultCharset());

    }
}
