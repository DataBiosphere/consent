package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.Test;

import java.util.Date;
import java.util.EnumSet;

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
    public void testInsert_AllFields() {
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

    @Test
    public void testInsert_AllEmailTypes() {
        EnumSet.allOf(EmailType.class).forEach(t -> {
            Date now = new Date();
            Integer mailId = mailMessageDAO.insert(
                    RandomStringUtils.randomAlphanumeric(10),
                    RandomUtils.nextInt(1, 1000),
                    RandomUtils.nextInt(1, 1000),
                    t.getTypeInt(),
                    now,
                    RandomStringUtils.randomAlphanumeric(10),
                    RandomStringUtils.randomAlphanumeric(10),
                    RandomUtils.nextInt(200, 399),
                    now
            );
            assertNotNull(mailId);
        });
    }


    @Test
    public void testInsert_NullEntityReferenceId() {
        Date now = new Date();
        Integer mailId = mailMessageDAO.insert(
                null,
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

    @Test
    public void testInsert_NullVoteId() {
        Date now = new Date();
        Integer mailId = mailMessageDAO.insert(
                RandomStringUtils.randomAlphanumeric(10),
                null,
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

    @Test
    public void testInsert_NullDateSent() {
        Date now = new Date();
        Integer mailId = mailMessageDAO.insert(
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(1, 1000),
                RandomUtils.nextInt(1, 1000),
                EmailType.COLLECT.getTypeInt(),
                null,
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(200, 399),
                now
        );
        assertNotNull(mailId);
    }

    @Test
    public void testInsert_NullSendGridResponse() {
        Date now = new Date();
        Integer mailId = mailMessageDAO.insert(
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(1, 1000),
                RandomUtils.nextInt(1, 1000),
                EmailType.COLLECT.getTypeInt(),
                now,
                RandomStringUtils.randomAlphanumeric(10),
                null,
                RandomUtils.nextInt(200, 399),
                now
        );
        assertNotNull(mailId);
    }

    @Test
    public void testInsert_NullSendGridStatus() {
        Date now = new Date();
        Integer mailId = mailMessageDAO.insert(
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(1, 1000),
                RandomUtils.nextInt(1, 1000),
                EmailType.COLLECT.getTypeInt(),
                now,
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                null,
                now
        );
        assertNotNull(mailId);
    }

    @Test(expected = UnableToExecuteStatementException.class)
    public void testInsert_MissingRequired() {
        Date now = new Date();
        mailMessageDAO.insert(
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(1, 1000),
                null,
                null,
                now,
                null,
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(200, 399),
                null
        );
    }
}
