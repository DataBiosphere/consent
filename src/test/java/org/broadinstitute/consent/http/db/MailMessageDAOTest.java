package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
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

    @Test
    public void testInsertAllFields() {
        Date now = new Date();
        Integer mailId = mailMessageDAO.insert(
            RandomStringUtils.randomAlphanumeric(10),
            RandomUtils.nextInt(1, 1000),
            RandomUtils.nextInt(1, 1000),
            EmailType.COLLECT.getTypeInt(),
            now,
            RandomStringUtils.randomAlphanumeric(10),
            RandomStringUtils.randomAlphanumeric(10),
            RandomUtils.nextInt(200, 399),
            now
        );
        assertNotNull(mailId);
    }
}
