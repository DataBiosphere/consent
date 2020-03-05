package org.broadinstitute.consent.http.db;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.junit.Assert;
import org.junit.Test;

public class DataAccessRequestDAOTest extends DAOTestFramework {

    @Test
    public void testCreate() {
        DataAccessRequest dar = createDtaAccessRequest();
        Assert.assertNotNull(dar);
    }

}
