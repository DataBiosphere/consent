package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.Test;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
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
        Instant now = Instant.now();
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
            Instant now = Instant.now();
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
        Instant now = Instant.now();
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
        Instant now = Instant.now();
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
        Instant now = Instant.now();
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
        Instant now = Instant.now();
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
        Instant now = Instant.now();
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
    public void testInsert_MissingUserId() {
        Instant now = Instant.now();
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

    @Test(expected = UnableToExecuteStatementException.class)
    public void testInsert_MissingEmailType() {
        Instant now = Instant.now();
        mailMessageDAO.insert(
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(1, 1000),
                RandomUtils.nextInt(1, 1000),
                null,
                now,
                null,
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(200, 399),
                null
        );
    }

    @Test(expected = UnableToExecuteStatementException.class)
    public void testInsert_MissingEmailText() {
        Instant now = Instant.now();
        mailMessageDAO.insert(
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(1, 1000),
                RandomUtils.nextInt(1, 1000),
                EmailType.COLLECT.getTypeInt(),
                now,
                null,
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(200, 399),
                null
        );
    }

    @Test(expected = UnableToExecuteStatementException.class)
    public void testInsert_MissingCreateDate() {
        Instant now = Instant.now();
        mailMessageDAO.insert(
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(1, 1000),
                RandomUtils.nextInt(1, 1000),
                EmailType.COLLECT.getTypeInt(),
                now,
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomUtils.nextInt(200, 399),
                null
        );
    }

    @Test
    public void testFetch() {
        EnumSet.allOf(EmailType.class).forEach(t -> {
            Instant now = Instant.now();
            mailMessageDAO.insert(
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
        });

        EnumSet.allOf(EmailType.class).forEach(t ->
                assertEquals(1, mailMessageDAO.fetchMessagesByType(t.getTypeInt(), 1, 0).size()));
    }

    @Test
    public void testFetchLimitAndOffset() {
        Instant now = Instant.now();
        mailMessageDAO.insert(
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

        List<MailMessage> mailMessageList = mailMessageDAO.fetchMessagesByType(EmailType.COLLECT.getTypeInt(), 1, 0);
        assertEquals(1, mailMessageList.size());

        List<MailMessage> mailMessageList2 = mailMessageDAO.fetchMessagesByType(EmailType.COLLECT.getTypeInt(), 1, 1);
        assertEquals(0, mailMessageList2.size());

        Integer mailId2 = mailMessageDAO.insert(
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

        List<MailMessage> mailMessageList3 = mailMessageDAO.fetchMessagesByType(EmailType.COLLECT.getTypeInt(), 1, 1);
        assertEquals(1, mailMessageList3.size());
        assertEquals(mailId2, mailMessageList3.get(0).getEmailId());

        List<MailMessage> mailMessageList4 = mailMessageDAO.fetchMessagesByType(EmailType.COLLECT.getTypeInt(), 20, 0);
        assertEquals(2, mailMessageList4.size());
    }

    @Test
    public void testFetchByCreateDate_with_limit_and_offset(){
        Date start = new Date();
        generateMessagesInTime(2, 1);
        Date end = new Date();
        List<MailMessage> mailMessageList = mailMessageDAO.fetchMessagesByCreateDate(start, end, 1, 0);
        assertEquals(1, mailMessageList.size());

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.DAY_OF_MONTH, -1*2*2);
        List<MailMessage> mailMessageList2 = mailMessageDAO.fetchMessagesByCreateDate(startCalendar.getTime(), end, 2, 0);
        assertEquals(2, mailMessageList2.size());

        List<MailMessage> mailMessageList3 = mailMessageDAO.fetchMessagesByCreateDate(startCalendar.getTime(), end, 2, 1);
        assertEquals(1, mailMessageList3.size());

        List<MailMessage> mailMessageList4 = mailMessageDAO.fetchMessagesByCreateDate(startCalendar.getTime(), end, 1, 0);
        assertEquals(1, mailMessageList4.size());
    }

    private void generateMessagesInTime(int number, int daysApart) {
        Calendar calendar = Calendar.getInstance();
        for (int x=0; x < number; x++){
            mailMessageDAO.insert(
                    RandomStringUtils.randomAlphanumeric(10),
                    RandomUtils.nextInt(1, 1000),
                    RandomUtils.nextInt(1, 1000),
                    EmailType.COLLECT.getTypeInt(),
                    calendar.toInstant(),
                    RandomStringUtils.randomAlphanumeric(10),
                    RandomStringUtils.randomAlphanumeric(10),
                    RandomUtils.nextInt(200, 399),
                    calendar.toInstant()
            );
            calendar.add(Calendar.DAY_OF_MONTH, -1*daysApart);
        }

    }
}
