package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class MailMessageDAOTest extends DAOTestHelper {

    @Test
    public void testExistsCollectDAREmailNegative() {
        Integer exists = mailMessageDAO.existsCollectDAREmail(
                RandomStringUtils.random(10, true, false),
                RandomStringUtils.random(10, true, false)
        );
        assertNull(exists);
    }

}
