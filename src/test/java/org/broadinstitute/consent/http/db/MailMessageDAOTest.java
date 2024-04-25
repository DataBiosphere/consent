package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MailMessageDAOTest extends DAOTestHelper {

  @Test
  void testInsert_AllFields() {
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
  void testInsert_AllEmailTypes() {
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
  void testInsert_NullEntityReferenceId() {
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
  void testInsert_NullVoteId() {
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
  void testInsert_NullDateSent() {
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
  void testInsert_NullSendGridResponse() {
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
  void testInsert_NullSendGridStatus() {
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

  @Test
  void testInsert_MissingUserId() {
    Instant now = Instant.now();
    assertThrows(UnableToExecuteStatementException.class, () -> {
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
    });
  }

  @Test
  void testInsert_MissingEmailType() {
    Instant now = Instant.now();
    assertThrows(UnableToExecuteStatementException.class, () -> {
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
    });
  }

  @Test
  void testInsert_MissingEmailText() {
    Instant now = Instant.now();
    assertThrows(UnableToExecuteStatementException.class, () -> {
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
    });
  }

  @Test
  void testInsert_MissingCreateDate() {
    Instant now = Instant.now();
    assertThrows(UnableToExecuteStatementException.class, () -> {
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
    });
  }

  @Test
  void testFetch() {
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
        assertEquals(1,
            mailMessageDAO.fetchMessagesByType(t.getTypeInt(), 1, 0).size()));
  }

  @Test
  void testFetchLimitAndOffset() {
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

    List<MailMessage> mailMessageList = mailMessageDAO.fetchMessagesByType(
        EmailType.COLLECT.getTypeInt(), 1, 0);
    assertEquals(1, mailMessageList.size());

    List<MailMessage> mailMessageList2 = mailMessageDAO.fetchMessagesByType(
        EmailType.COLLECT.getTypeInt(), 1, 1);
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

    List<MailMessage> mailMessageList3 = mailMessageDAO.fetchMessagesByType(
        EmailType.COLLECT.getTypeInt(), 1, 1);
    assertEquals(1, mailMessageList3.size());
    assertEquals(mailId2, mailMessageList3.get(0).getEmailId());

    List<MailMessage> mailMessageList4 = mailMessageDAO.fetchMessagesByType(
        EmailType.COLLECT.getTypeInt(), 20, 0);
    assertEquals(2, mailMessageList4.size());
  }

  @Test
  void testFetchByCreateDate_with_limit_and_offset() {
    // To fully test mail messages, we'll need a minimum of two to test limits and offsets.
    Instant now = Instant.now();
    Instant yesterday = now.minus(1, ChronoUnit.DAYS);
    MailMessage messageToday = generateMessage(now);
    MailMessage messageYesterday = generateMessage(yesterday);

    // We'll use these times to search with
    Instant yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

    // Find messages from beginning of today to the beginning of tomorrow. Should return `messageToday`
    List<MailMessage> messages = mailMessageDAO
        .fetchMessagesByCreateDate(Date.from(todayStart), Date.from(tomorrowStart), 1, 0);
    assertEquals(1, messages.size());
    assertEquals(messageToday.getEmailId(), messages.get(0).getEmailId());

    // Find messages from beginning of yesterday to tomorrow. Should return both messages.
    // Order is create date descending, so today is first, yesterday second.
    List<MailMessage> messages2 = mailMessageDAO
        .fetchMessagesByCreateDate(Date.from(yesterdayStart), Date.from(tomorrowStart), 2, 0);
    assertEquals(2, messages2.size());
    assertEquals(messageToday.getEmailId(), messages2.get(0).getEmailId());
    assertEquals(messageYesterday.getEmailId(), messages2.get(1).getEmailId());

    // Find messages from beginning of yesterday to tomorrow, offset by 1. Since messages are
    // ordered by create date descending, offset should trim today's message and only return
    // yesterday's message.
    List<MailMessage> messages3 = mailMessageDAO
        .fetchMessagesByCreateDate(Date.from(yesterdayStart), Date.from(tomorrowStart), 2, 1);
    assertEquals(1, messages3.size());
    assertEquals(messageYesterday.getEmailId(), messages3.get(0).getEmailId());

    // Find messages from beginning of yesterday to beginning today. Should return yesterday's message.
    List<MailMessage> messages4 = mailMessageDAO
        .fetchMessagesByCreateDate(Date.from(yesterdayStart), Date.from(todayStart), 2, 0);
    assertEquals(1, messages4.size());
    assertEquals(messageYesterday.getEmailId(), messages4.get(0).getEmailId());
  }

  private MailMessage generateMessage(Instant instant) {
    Integer messageId = mailMessageDAO.insert(
        RandomStringUtils.randomAlphanumeric(10),
        RandomUtils.nextInt(1, 1000),
        RandomUtils.nextInt(1, 1000),
        EmailType.COLLECT.getTypeInt(),
        instant,
        RandomStringUtils.randomAlphanumeric(10),
        RandomStringUtils.randomAlphanumeric(10),
        RandomUtils.nextInt(200, 399),
        instant
    );
    return mailMessageDAO.fetchMessageById(messageId);
  }
}
