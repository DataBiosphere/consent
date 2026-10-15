package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.broadinstitute.consent.http.models.mail.MailMessageInsert;
import org.broadinstitute.consent.http.models.mail.MailSend;
import org.broadinstitute.consent.http.models.mail.MailSendRecipient;
import org.broadinstitute.consent.http.models.mail.MailSendSearch;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MailMessageDAOTest extends DAOTestHelper {

  /** Width of email_entity.entity_reference_id. */
  private static final int ENTITY_REFERENCE_ID_COLUMN_WIDTH = 255;

  @Test
  void testInsert_AllFields() {
    User user = createUser();
    Instant now = Instant.now();
    MailMessage mail =
        mailMessageDAO.insert(
            new MailMessageInsert(
                randomAlphanumeric(10),
                randomInt(1, 1000),
                user.getUserId(),
                EmailType.COLLECT.getTypeInt(),
                Date.from(now),
                randomAlphanumeric(10),
                randomAlphanumeric(10),
                randomInt(200, 399),
                null));
    assertNotNull(mail);
  }

  @Test
  void testInsert_AllEmailTypes() {
    User user = createUser();
    EnumSet.allOf(EmailType.class)
        .forEach(
            t -> {
              Instant now = Instant.now();
              MailMessage mail =
                  mailMessageDAO.insert(
                      new MailMessageInsert(
                          randomAlphanumeric(10),
                          randomInt(1, 1000),
                          user.getUserId(),
                          t.getTypeInt(),
                          Date.from(now),
                          randomAlphanumeric(10),
                          randomAlphanumeric(10),
                          randomInt(200, 399),
                          null));
              assertNotNull(mail);
            });
  }

  @Test
  void testInsert_NullEntityReferenceId() {
    User user = createUser();
    Instant now = Instant.now();
    MailMessage mail =
        mailMessageDAO.insert(
            new MailMessageInsert(
                null,
                randomInt(1, 1000),
                user.getUserId(),
                EmailType.COLLECT.getTypeInt(),
                Date.from(now),
                randomAlphanumeric(10),
                randomAlphanumeric(10),
                randomInt(200, 399),
                null));
    assertNotNull(mail);
  }

  /**
   * Pins the width relied on by {@code EntityReferenceIdLengthTest}: a reference id that fills the
   * column is stored intact.
   */
  @Test
  void testInsert_MaxLengthEntityReferenceId() {
    User user = createUser();
    String entityReferenceId = randomAlphanumeric(ENTITY_REFERENCE_ID_COLUMN_WIDTH);
    MailMessage mail =
        mailMessageDAO.insert(
            new MailMessageInsert(
                entityReferenceId,
                randomInt(1, 1000),
                user.getUserId(),
                EmailType.COLLECT.getTypeInt(),
                Date.from(Instant.now()),
                randomAlphanumeric(10),
                randomAlphanumeric(10),
                randomInt(200, 399),
                null));
    assertNotNull(mail);
    assertEquals(entityReferenceId, mail.entityReferenceId());
  }

  /**
   * A reference id longer than the column is not truncated, it fails the insert. EmailService hands
   * the message to SendGrid before inserting, so any MailMessage returning an over-long entity
   * reference id sends the email and then loses the record of having sent it. See {@code
   * EntityReferenceIdLengthTest}, which guards each MailMessage implementation against this.
   */
  @Test
  void testInsert_EntityReferenceIdLongerThanColumnFails() {
    Integer columnWidth =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        """
                        SELECT character_maximum_length
                        FROM information_schema.columns
                        WHERE table_name = 'email_entity' AND column_name = 'entity_reference_id'
                        """)
                    .mapTo(Integer.class)
                    .one());
    assertEquals(ENTITY_REFERENCE_ID_COLUMN_WIDTH, columnWidth);

    User user = createUser();
    MailMessageInsert mailMessageInsert =
        new MailMessageInsert(
            randomAlphanumeric(ENTITY_REFERENCE_ID_COLUMN_WIDTH + 1),
            randomInt(1, 1000),
            user.getUserId(),
            EmailType.COLLECT.getTypeInt(),
            Date.from(Instant.now()),
            randomAlphanumeric(10),
            randomAlphanumeric(10),
            randomInt(200, 399),
            null);
    assertThrows(
        UnableToExecuteStatementException.class, () -> mailMessageDAO.insert(mailMessageInsert));
  }

  @Test
  void testInsert_NullVoteId() {
    User user = createUser();
    Instant now = Instant.now();
    MailMessage mail =
        mailMessageDAO.insert(
            new MailMessageInsert(
                randomAlphanumeric(10),
                null,
                user.getUserId(),
                EmailType.COLLECT.getTypeInt(),
                Date.from(now),
                randomAlphanumeric(10),
                randomAlphanumeric(10),
                randomInt(200, 399),
                null));
    assertNotNull(mail);
  }

  @Test
  void testInsert_NullDateSent() {
    User user = createUser();
    MailMessage mail =
        mailMessageDAO.insert(
            new MailMessageInsert(
                randomAlphanumeric(10),
                randomInt(1, 1000),
                user.getUserId(),
                EmailType.COLLECT.getTypeInt(),
                null,
                randomAlphanumeric(10),
                randomAlphanumeric(10),
                randomInt(200, 399),
                null));
    assertNotNull(mail);
  }

  @Test
  void testInsert_NullSendGridResponse() {
    User user = createUser();
    Instant now = Instant.now();
    MailMessage mail =
        mailMessageDAO.insert(
            new MailMessageInsert(
                randomAlphanumeric(10),
                randomInt(1, 1000),
                user.getUserId(),
                EmailType.COLLECT.getTypeInt(),
                Date.from(now),
                randomAlphanumeric(10),
                null,
                randomInt(200, 399),
                null));
    assertNotNull(mail);
  }

  @Test
  void testInsert_NullSendGridStatus() {
    User user = createUser();
    Instant now = Instant.now();
    MailMessage mail =
        mailMessageDAO.insert(
            new MailMessageInsert(
                randomAlphanumeric(10),
                randomInt(1, 1000),
                user.getUserId(),
                EmailType.COLLECT.getTypeInt(),
                Date.from(now),
                randomAlphanumeric(10),
                randomAlphanumeric(10),
                null,
                null));
    assertNotNull(mail);
  }

  @Test
  void testInsert_MissingUserId() {
    Instant now = Instant.now();
    String entityReferenceId = randomAlphanumeric(10);
    Integer voteId = randomInt(1, 1000);
    String sendGridResponse = randomAlphanumeric(10);
    Integer sendGridStatus = randomInt(200, 399);
    MailMessageInsert mailMessageInsert =
        new MailMessageInsert(
            entityReferenceId,
            voteId,
            null,
            null,
            Date.from(now),
            null,
            sendGridResponse,
            sendGridStatus,
            null);
    assertThrows(
        UnableToExecuteStatementException.class, () -> mailMessageDAO.insert(mailMessageInsert));
  }

  @Test
  void testInsert_MissingEmailType() {
    User user = createUser();
    Instant now = Instant.now();
    String entityReferenceId = randomAlphanumeric(10);
    Integer voteId = randomInt(1, 1000);
    String sendGridResponse = randomAlphanumeric(10);
    Integer sendGridStatus = randomInt(200, 399);
    MailMessageInsert mailMessageInsert =
        new MailMessageInsert(
            entityReferenceId,
            voteId,
            user.getUserId(),
            null,
            Date.from(now),
            null,
            sendGridResponse,
            sendGridStatus,
            null);
    assertThrows(
        UnableToExecuteStatementException.class, () -> mailMessageDAO.insert(mailMessageInsert));
  }

  @Test
  void testInsert_MissingEmailText() {
    User user = createUser();
    Instant now = Instant.now();
    String entityReferenceId = randomAlphanumeric(10);
    Integer voteId = randomInt(1, 1000);
    Integer emailType = EmailType.COLLECT.getTypeInt();
    String sendGridResponse = randomAlphanumeric(10);
    Integer sendGridStatus = randomInt(200, 399);
    MailMessageInsert mailMessageInsert =
        new MailMessageInsert(
            entityReferenceId,
            voteId,
            user.getUserId(),
            emailType,
            Date.from(now),
            null,
            sendGridResponse,
            sendGridStatus,
            null);
    assertThrows(
        UnableToExecuteStatementException.class, () -> mailMessageDAO.insert(mailMessageInsert));
  }

  @Test
  void testInsert_ProvidesCreateDateFromDatabase() {
    User user = createUser();
    Instant historicalInstant = Instant.parse("2000-01-01T00:00:00Z");
    String entityReferenceId = randomAlphanumeric(10);
    Integer voteId = randomInt(1, 1000);
    Integer emailType = EmailType.COLLECT.getTypeInt();
    String emailText = randomAlphanumeric(10);
    String sendGridResponse = randomAlphanumeric(10);
    Integer sendGridStatus = randomInt(200, 399);
    MailMessage savedMessage =
        mailMessageDAO.insert(
            new MailMessageInsert(
                entityReferenceId,
                voteId,
                user.getUserId(),
                emailType,
                Date.from(historicalInstant),
                emailText,
                sendGridResponse,
                sendGridStatus,
                null));
    assertNotNull(savedMessage.createDate());
    assertTrue(savedMessage.createDate().toInstant().isAfter(historicalInstant));
  }

  @Test
  void testFetch() {
    User user = createUser();
    EnumSet.allOf(EmailType.class)
        .forEach(
            t -> {
              Instant now = Instant.now();
              mailMessageDAO.insert(
                  new MailMessageInsert(
                      randomAlphanumeric(10),
                      randomInt(1, 1000),
                      user.getUserId(),
                      t.getTypeInt(),
                      Date.from(now),
                      randomAlphanumeric(10),
                      randomAlphanumeric(10),
                      randomInt(200, 399),
                      null));
            });

    EnumSet.allOf(EmailType.class)
        .forEach(
            t -> assertEquals(1, mailMessageDAO.fetchMessagesByType(t.getTypeInt(), 1, 0).size()));
  }

  @Test
  void testFetchLimitAndOffset() {
    User user = createUser();
    Instant now = Instant.now();
    MailMessage firstMail = generateMessage(user, now.minus(1, ChronoUnit.HOURS));

    List<MailMessage> mailMessageList =
        mailMessageDAO.fetchMessagesByType(EmailType.COLLECT.getTypeInt(), 1, 0);
    assertEquals(1, mailMessageList.size());

    List<MailMessage> mailMessageList2 =
        mailMessageDAO.fetchMessagesByType(EmailType.COLLECT.getTypeInt(), 1, 1);
    assertEquals(0, mailMessageList2.size());

    generateMessage(user, now);

    List<MailMessage> mailMessageList3 =
        mailMessageDAO.fetchMessagesByType(EmailType.COLLECT.getTypeInt(), 1, 1);
    assertEquals(1, mailMessageList3.size());
    assertEquals(firstMail.emailId(), mailMessageList3.getFirst().emailId());

    List<MailMessage> mailMessageList4 =
        mailMessageDAO.fetchMessagesByType(EmailType.COLLECT.getTypeInt(), 20, 0);
    assertEquals(2, mailMessageList4.size());
  }

  @Test
  void testFetchByUserId() {
    Instant now = Instant.now();
    User user = createUser();
    mailMessageDAO.insert(
        new MailMessageInsert(
            randomAlphanumeric(10),
            randomInt(1, 1000),
            user.getUserId(),
            EmailType.COLLECT.getTypeInt(),
            Date.from(now),
            randomAlphanumeric(10),
            randomAlphanumeric(10),
            randomInt(200, 399),
            null));

    List<MailMessage> mailMessageList =
        mailMessageDAO.fetchMessagesByUserId(user.getUserId(), 10, 0);
    assertEquals(1, mailMessageList.size());
    assertEquals(user.getUserId(), mailMessageList.getFirst().userId());

    mailMessageDAO.insert(
        new MailMessageInsert(
            randomAlphanumeric(10),
            randomInt(1, 1000),
            user.getUserId(),
            EmailType.COLLECT.getTypeInt(),
            Date.from(now),
            randomAlphanumeric(10),
            randomAlphanumeric(10),
            randomInt(200, 399),
            null));
    List<MailMessage> mailMessageList2 =
        mailMessageDAO.fetchMessagesByUserId(user.getUserId(), 10, 0);
    assertEquals(2, mailMessageList2.size());
    assertEquals(user.getUserId(), mailMessageList2.getFirst().userId());

    User user2 = createUser();
    List<MailMessage> mailMessageList3 =
        mailMessageDAO.fetchMessagesByUserId(user2.getUserId(), 10, 0);
    assertEquals(0, mailMessageList3.size());
  }

  @Test
  void testFetchByCreateDate_with_limit_and_offset() {
    // To fully test mail messages, we'll need a minimum of two to test limits and offsets.
    Instant now = Instant.now();
    Instant yesterday = now.minus(1, ChronoUnit.DAYS);
    MailMessage messageToday = generateMessage(now);
    MailMessage messageYesterday = generateMessage(yesterday);

    // We'll use these times to search with
    Instant yesterdayStart =
        LocalDate.now(ZoneOffset.UTC).minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
    Instant tomorrowStart =
        LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

    // Find messages from beginning of today to the beginning of tomorrow. Should return
    // `messageToday`
    List<MailMessage> messages =
        mailMessageDAO.fetchMessagesByCreateDate(
            Date.from(todayStart), Date.from(tomorrowStart), 1, 0);
    assertEquals(1, messages.size());
    assertEquals(messageToday.emailId(), messages.getFirst().emailId());

    // Find messages from beginning of yesterday to tomorrow. Should return both messages.
    // Order is create date descending, so today is first, yesterday second.
    List<MailMessage> messages2 =
        mailMessageDAO.fetchMessagesByCreateDate(
            Date.from(yesterdayStart), Date.from(tomorrowStart), 2, 0);
    assertEquals(2, messages2.size());
    assertEquals(messageToday.emailId(), messages2.get(0).emailId());
    assertEquals(messageYesterday.emailId(), messages2.get(1).emailId());

    // Find messages from beginning of yesterday to tomorrow, offset by 1. Since messages are
    // ordered by create date descending, offset should trim today's message and only return
    // yesterday's message.
    List<MailMessage> messages3 =
        mailMessageDAO.fetchMessagesByCreateDate(
            Date.from(yesterdayStart), Date.from(tomorrowStart), 2, 1);
    assertEquals(1, messages3.size());
    assertEquals(messageYesterday.emailId(), messages3.getFirst().emailId());

    // Find messages from beginning of yesterday to beginning today. Should return yesterday's
    // message.
    List<MailMessage> messages4 =
        mailMessageDAO.fetchMessagesByCreateDate(
            Date.from(yesterdayStart), Date.from(todayStart), 2, 0);
    assertEquals(1, messages4.size());
    assertEquals(messageYesterday.emailId(), messages4.getFirst().emailId());
  }

  @Test
  void testInsert_stores_the_send_id() {
    UUID sendId = UUID.randomUUID();
    MailMessage saved =
        mailMessageDAO.insert(
            new MailMessageInsert(
                "DAR-1",
                null,
                createUser().getUserId(),
                EmailType.NEW_DAR.getTypeInt(),
                null,
                randomAlphanumeric(10),
                null,
                null,
                sendId));

    UUID stored =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT send_id FROM email_entity WHERE email_entity_id = :id")
                    .bind("id", saved.emailId())
                    .mapTo(UUID.class)
                    .one());

    assertEquals(sendId, stored);
  }

  @Test
  void testInsert_stores_a_null_send_id() {
    MailMessage saved =
        mailMessageDAO.insert(
            new MailMessageInsert(
                "DAR-1",
                null,
                createUser().getUserId(),
                EmailType.NEW_DAR.getTypeInt(),
                null,
                randomAlphanumeric(10),
                null,
                null,
                null));

    boolean storedNull =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT send_id IS NULL FROM email_entity WHERE email_entity_id = :id")
                    .bind("id", saved.emailId())
                    .mapTo(Boolean.class)
                    .one());

    assertTrue(storedNull);
  }

  @Test
  void testInsert() {
    User user = createUser();
    String entityReferenceId = randomAlphanumeric(10);
    Integer voteId = randomInt(1, 1000);
    Integer emailType = EmailType.NEW_DAR.getTypeInt();
    Instant now = Instant.now();
    Date nowDate = Date.from(now);
    String emailText = randomAlphanumeric(1000);
    String sendGridResponse = randomAlphanumeric(1000);
    Integer sendGridStatus = 200;
    MailMessageInsert unsavedMessage =
        new MailMessageInsert(
            entityReferenceId,
            voteId,
            user.getUserId(),
            emailType,
            nowDate,
            emailText,
            sendGridResponse,
            sendGridStatus,
            null);

    MailMessage savedMessage = mailMessageDAO.insert(unsavedMessage);
    assertNotNull(savedMessage);
    assertNotNull(savedMessage.emailId());
    assertEquals(entityReferenceId, savedMessage.entityReferenceId());
    assertEquals(user.getUserId(), savedMessage.userId());
    assertEquals(emailType, savedMessage.emailType());
    assertEquals(nowDate, savedMessage.dateSent());
    assertEquals(emailText, savedMessage.emailText());
    assertEquals(sendGridStatus, savedMessage.sendgridStatus());
    assertEquals(sendGridResponse, savedMessage.sendgridResponse());
    assertNotNull(savedMessage.createDate());
  }

  @Test
  void testFetchSendsByCreateDate_groups_the_recipients_of_one_send() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
    User carol = createUserNamed("Carol");
    User alice = createUserNamed("Alice");
    User bob = createUserNamed("Bob");
    MailMessage earliest = generateSendRow(carol, EmailType.NEW_DAR, "DAR-1", first);
    generateSendRow(alice, EmailType.NEW_DAR, "DAR-1", first.plus(1, ChronoUnit.MINUTES));
    MailMessage latest =
        generateSendRow(bob, EmailType.NEW_DAR, "DAR-1", first.plus(2, ChronoUnit.MINUTES));

    List<MailSend> sends = fetchSendsAroundNow();

    assertEquals(
        List.of(
            new MailSend(
                earliest.emailId(),
                EmailType.NEW_DAR.getTypeInt(),
                "DAR-1",
                earliest.createDate(),
                latest.createDate(),
                3,
                List.of(
                    new MailSendRecipient(alice.getUserId(), "Alice", true),
                    new MailSendRecipient(bob.getUserId(), "Bob", true),
                    new MailSendRecipient(carol.getUserId(), "Carol", true)),
                null,
                List.of())),
        sends);
  }

  @Test
  void testFetchSendsByCreateDate_caps_the_recipients_but_counts_them_all() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    User carol = createUserNamed("Carol");
    User alice = createUserNamed("Alice");
    User bob = createUserNamed("Bob");
    generateSendRow(carol, EmailType.NEW_DAR, "DAR-1", first);
    generateSendRow(alice, EmailType.NEW_DAR, "DAR-1", first);
    generateSendRow(bob, EmailType.NEW_DAR, "DAR-1", first);

    MailSend send =
        mailMessageDAO
            .fetchSendsByCreateDate(
                Date.from(first.minus(1, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                10,
                0,
                2,
                MailSendSearch.NONE)
            .getFirst();

    assertEquals(3, send.recipientCount());
    assertEquals(
        List.of(
            new MailSendRecipient(alice.getUserId(), "Alice", true),
            new MailSendRecipient(bob.getUserId(), "Bob", true)),
        send.recipients());
  }

  @Test
  void testFetchSendsByCreateDate_starts_a_new_send_after_a_ten_minute_gap() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(createUser(), EmailType.NEW_DAR, "DAR-1", first);
    generateSendRow(createUser(), EmailType.NEW_DAR, "DAR-1", first.plus(10, ChronoUnit.MINUTES));
    generateSendRow(createUser(), EmailType.NEW_DAR, "DAR-1", first.plus(21, ChronoUnit.MINUTES));

    List<Integer> recipientCounts =
        fetchSendsAroundNow().stream().map(MailSend::recipientCount).toList();

    assertEquals(List.of(1, 2), recipientCounts);
  }

  @Test
  void testFetchSendsByCreateDate_splits_day_only_rows_by_their_sent_time() {
    Instant day = Instant.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
    for (int hour : List.of(9, 15)) {
      MailMessage row =
          generateSendRow(
              createUser(), EmailType.NEW_DAR, "DAR-1", day.plus(hour, ChronoUnit.HOURS));
      setCreateDate(row.emailId(), day);
    }

    List<MailSend> sends =
        mailMessageDAO.fetchSendsByCreateDate(
            Date.from(day.minus(1, ChronoUnit.DAYS)),
            Date.from(day.plus(1, ChronoUnit.DAYS)),
            100,
            0,
            100,
            MailSendSearch.NONE);

    assertEquals(2, sends.size());
  }

  @Test
  void testFetchSendsByCreateDate_marks_unsent_recipients() {
    User user = createUser();
    MailMessage unsent =
        mailMessageDAO.insert(
            new MailMessageInsert(
                "DAR-1",
                null,
                user.getUserId(),
                EmailType.NEW_DAR.getTypeInt(),
                null,
                randomAlphanumeric(10),
                null,
                null,
                null));
    setCreateDate(unsent.emailId(), Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(
        List.of(new MailSendRecipient(user.getUserId(), user.getDisplayName(), false)),
        send.recipients());
    assertEquals(unsent.emailId(), send.sendId());
  }

  @Test
  void testFetchSendsByCreateDate_lists_a_user_with_two_emails_in_a_send_once() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    User alice = createUserNamed("Alice");
    generateSendRow(alice, EmailType.NEW_DAR, "DAR-1", first);
    generateSendRow(alice, EmailType.NEW_DAR, "DAR-1", first.plus(1, ChronoUnit.MINUTES));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(1, send.recipientCount());
    assertEquals(
        List.of(new MailSendRecipient(alice.getUserId(), "Alice", true)), send.recipients());
  }

  @Test
  void testFetchSendsByCreateDate_keeps_a_send_when_no_recipients_are_listed() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(createUser(), EmailType.NEW_DAR, "DAR-1", first);

    MailSend send =
        mailMessageDAO
            .fetchSendsByCreateDate(
                Date.from(first.minus(1, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                10,
                0,
                0,
                MailSendSearch.NONE)
            .getFirst();

    assertEquals(1, send.recipientCount());
    assertEquals(List.of(), send.recipients());
  }

  @Test
  void testFetchSendsByCreateDate_keeps_emails_without_a_reference_apart() {
    Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(createUser(), EmailType.NEW_DAR, null, now);
    generateSendRow(createUser(), EmailType.NEW_DAR, null, now);

    assertEquals(
        List.of(1, 1), fetchSendsAroundNow().stream().map(MailSend::recipientCount).toList());
  }

  @Test
  void testFetchSendsByCreateDate_groups_a_send_id_however_long_the_send_takes() {
    Instant first = Instant.now().minus(2, ChronoUnit.HOURS);
    UUID sendId = UUID.randomUUID();
    generateSendRow(createUser(), EmailType.NEW_STUDY_DIGEST, "2026-10-09", first, sendId);
    generateSendRow(
        createUser(),
        EmailType.NEW_STUDY_DIGEST,
        "2026-10-09",
        first.plus(30, ChronoUnit.MINUTES),
        sendId);

    assertEquals(List.of(2), fetchSendsAroundNow().stream().map(MailSend::recipientCount).toList());
  }

  @Test
  void testFetchSendsByCreateDate_separates_send_ids_sent_close_together() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(createUser(), EmailType.NEW_DAR, "DAR-1", first, UUID.randomUUID());
    generateSendRow(
        createUser(),
        EmailType.NEW_DAR,
        "DAR-1",
        first.plus(1, ChronoUnit.MINUTES),
        UUID.randomUUID());

    assertEquals(
        List.of(1, 1), fetchSendsAroundNow().stream().map(MailSend::recipientCount).toList());
  }

  @Test
  void testFetchSendsByCreateDate_groups_a_send_id_without_a_reference() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    UUID sendId = UUID.randomUUID();
    generateSendRow(createUser(), EmailType.NEW_DAR, null, first, sendId);
    generateSendRow(createUser(), EmailType.NEW_DAR, null, first, sendId);

    assertEquals(List.of(2), fetchSendsAroundNow().stream().map(MailSend::recipientCount).toList());
  }

  @Test
  void testFetchSendsByCreateDate_separates_types_and_entity_references() {
    Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(createUser(), EmailType.NEW_DAR, "DAR-1", now);
    generateSendRow(createUser(), EmailType.NEW_DAR, "DAR-2", now);
    generateSendRow(createUser(), EmailType.NEW_CASE, "DAR-1", now);

    assertEquals(3, fetchSendsAroundNow().size());
  }

  @Test
  void testFetchSendsByCreateDate_pages_by_send() {
    Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
    List<String> referencesNewestFirst = List.of("DAR-3", "DAR-2", "DAR-1");
    for (int i = 0; i < referencesNewestFirst.size(); i++) {
      Instant created = now.minus(i, ChronoUnit.MINUTES);
      generateSendRow(createUser(), EmailType.NEW_DAR, referencesNewestFirst.get(i), created);
      generateSendRow(createUser(), EmailType.NEW_DAR, referencesNewestFirst.get(i), created);
    }
    Date start = Date.from(now.minus(1, ChronoUnit.HOURS));
    Date end = Date.from(Instant.now());

    List<String> pagedReferences =
        IntStream.range(0, 3)
            .mapToObj(
                offset ->
                    mailMessageDAO
                        .fetchSendsByCreateDate(start, end, 1, offset, 100, MailSendSearch.NONE)
                        .getFirst()
                        .entityReferenceId())
            .toList();

    assertEquals(referencesNewestFirst, pagedReferences);
  }

  @Test
  void testFetchSendsByCreateDate_excludes_the_end_instant_in_either_order() {
    Instant end = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant start = end.minus(1, ChronoUnit.HOURS);
    MailMessage inside =
        generateSendRow(createUser(), EmailType.NEW_DAR, "DAR-1", end.minus(1, ChronoUnit.MINUTES));
    generateSendRow(createUser(), EmailType.NEW_DAR, "DAR-2", end);

    List<Integer> forward =
        mailMessageDAO
            .fetchSendsByCreateDate(
                Date.from(start), Date.from(end), 10, 0, 100, MailSendSearch.NONE)
            .stream()
            .map(MailSend::sendId)
            .toList();
    List<Integer> reversed =
        mailMessageDAO
            .fetchSendsByCreateDate(
                Date.from(end), Date.from(start), 10, 0, 100, MailSendSearch.NONE)
            .stream()
            .map(MailSend::sendId)
            .toList();

    assertEquals(List.of(inside.emailId()), forward);
    assertEquals(forward, reversed);
  }

  @Test
  void testFetchSendsByCreateDate_reads_a_dar_code_as_its_submitted_dar() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDar(user, collectionId);
    Dataset first = createDatasetFor(dar);
    Dataset second = createDatasetFor(dar);
    createDatasetFor(createProgressReport(user, dar));
    createDatasetFor(createDar(user, collectionId, null));
    DataAccessRequest archived = createDar(user, collectionId);
    createDatasetFor(archived);
    dataAccessRequestDAO.archiveByReferenceIds(List.of(archived.getReferenceId()));
    generateSendRow(user, EmailType.NEW_DAR, darCode, Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(darCode, send.darCode());
    assertEquals(
        List.of(first.getDatasetIdentifier(), second.getDatasetIdentifier()),
        send.datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_reads_a_progress_report_reference_as_its_parent_dar() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest parent = createDar(user, collectionId);
    createDatasetFor(parent);
    DataAccessRequest progressReport = createProgressReport(user, parent);
    Dataset reported = createDatasetFor(progressReport);
    generateSendRow(
        user,
        EmailType.SO_PROGRESS_REPORT_SUBMITTED,
        progressReport.getReferenceId(),
        Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(darCode, send.darCode());
    assertEquals(List.of(reported.getDatasetIdentifier()), send.datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_reads_an_election_id_as_its_dar_and_dataset() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDar(user, collectionId);
    createDatasetFor(dar);
    Dataset elected = createDatasetFor(dar);
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            new Date(),
            dar.getReferenceId(),
            elected.getDatasetId());
    generateSendRow(
        user, EmailType.REMINDER, electionId.toString(), Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(darCode, send.darCode());
    assertEquals(List.of(elected.getDatasetIdentifier()), send.datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_reads_a_ten_digit_election_id() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDar(user, collectionId);
    Dataset elected = createDatasetFor(dar);
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            new Date(),
            dar.getReferenceId(),
            elected.getDatasetId());
    long largeId = 10_000_000_000L + electionId;
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE election SET election_id = :largeId WHERE election_id = :id")
                .bind("largeId", largeId)
                .bind("id", electionId)
                .execute());
    generateSendRow(
        user, EmailType.REMINDER, Long.toString(largeId), Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(darCode, send.darCode());
    assertEquals(List.of(elected.getDatasetIdentifier()), send.datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_reads_an_election_without_a_dataset_as_its_dars_datasets() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDar(user, collectionId);
    Dataset requested = createDatasetFor(dar);
    Integer electionId =
        electionDAO.insertElection(
            ElectionType.DATA_ACCESS.getValue(),
            ElectionStatus.OPEN.getValue(),
            new Date(),
            dar.getReferenceId(),
            null);
    generateSendRow(
        user, EmailType.REMINDER, electionId.toString(), Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(darCode, send.darCode());
    assertEquals(List.of(requested.getDatasetIdentifier()), send.datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_reads_an_older_dar_reference_on_a_dar_code_type() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDar(user, collectionId);
    Dataset requested = createDatasetFor(dar);
    generateSendRow(
        user, EmailType.NEW_DAR, dar.getReferenceId(), Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(darCode, send.darCode());
    assertEquals(List.of(requested.getDatasetIdentifier()), send.datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_reads_a_duos_id_and_a_dataset_name() {
    Dataset approved = createDataset();
    Dataset submitted = createDataset();
    Instant created = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(
        createUser(), EmailType.DATASET_APPROVED, approved.getDatasetIdentifier(), created);
    generateSendRow(
        createUser(), EmailType.NEW_DATASET, submitted.getName(), created.plusSeconds(1));

    List<MailSend> sends = fetchSendsAroundNow();

    assertEquals(
        List.of(
            List.of(submitted.getDatasetIdentifier()), List.of(approved.getDatasetIdentifier())),
        sends.stream().map(MailSend::datasetIdentifiers).toList());
    assertEquals(Arrays.asList(null, null), sends.stream().map(MailSend::darCode).toList());
  }

  @Test
  void testFetchSendsByCreateDate_reads_an_older_dataset_name_on_a_duos_id_type() {
    Dataset dataset = createDataset();
    generateSendRow(
        createUser(),
        EmailType.DATASET_DENIED,
        dataset.getName(),
        Instant.now().minus(1, ChronoUnit.HOURS));

    assertEquals(
        List.of(dataset.getDatasetIdentifier()),
        fetchSendsAroundNow().getFirst().datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_reads_an_older_name_shaped_like_a_duos_id_as_a_name() {
    Dataset named = createDataset();
    String name = "DUOS-" + randomInt(100_000, 999_999);
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE dataset SET name = :name WHERE dataset_id = :id")
                .bind("name", name)
                .bind("id", named.getDatasetId())
                .execute());
    Instant beforeDuosIds = Instant.parse("2026-07-01T12:00:00Z");
    generateSendRow(createUser(), EmailType.DATASET_DENIED, name, beforeDuosIds);

    List<MailSend> sends =
        mailMessageDAO.fetchSendsByCreateDate(
            Date.from(beforeDuosIds.minus(1, ChronoUnit.DAYS)),
            Date.from(beforeDuosIds.plus(1, ChronoUnit.DAYS)),
            100,
            0,
            100,
            MailSendSearch.NONE);

    assertEquals(List.of(named.getDatasetIdentifier()), sends.getFirst().datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_reads_a_study_uuid_as_its_datasets() {
    User user = createUser();
    UUID studyUuid = UUID.randomUUID();
    Integer studyId =
        studyDAO.insertStudy(
            "Study_" + randomAlphabetic(10),
            "description",
            "pi",
            "pi@example.com",
            List.of(),
            true,
            user.getUserId(),
            Instant.now(),
            studyUuid);
    Dataset dataset = createDataset();
    datasetDAO.updateStudyId(dataset.getDatasetId(), studyId);
    generateSendRow(
        user,
        EmailType.NEW_STUDY_REGISTRATION_CONFIRMATION,
        studyUuid.toString(),
        Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertNull(send.darCode());
    assertEquals(List.of(dataset.getDatasetIdentifier()), send.datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_lists_the_datasets_a_send_recorded() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequest dar = createDar(user, collectionId);
    Dataset approved = createDatasetFor(dar);
    Dataset alsoApproved = createDatasetFor(dar);
    createDatasetFor(dar);
    UUID sendId = UUID.randomUUID();
    Instant sentAt = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(
        user,
        EmailType.DATA_CUSTODIAN_APPROVAL,
        darCode,
        sentAt,
        sendId,
        List.of(approved.getDatasetId()));
    generateSendRow(
        createUser(),
        EmailType.DATA_CUSTODIAN_APPROVAL,
        darCode,
        sentAt,
        sendId,
        List.of(alsoApproved.getDatasetId()));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(darCode, send.darCode());
    assertEquals(
        List.of(approved.getDatasetIdentifier(), alsoApproved.getDatasetIdentifier()),
        send.datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_does_not_look_up_a_send_that_recorded_no_datasets() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    createDatasetFor(createDar(user, collectionId));
    generateSendRow(
        user,
        EmailType.NEW_CASE,
        darCode,
        Instant.now().minus(1, ChronoUnit.HOURS),
        UUID.randomUUID(),
        List.of());

    assertEquals(List.of(), fetchSendsAroundNow().getFirst().datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_reads_an_older_dar_code_on_a_reference_id_type() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    Dataset requested = createDatasetFor(createDar(user, collectionId));
    generateSendRow(user, EmailType.DAR_EXPIRED, darCode, Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertEquals(darCode, send.darCode());
    assertEquals(List.of(requested.getDatasetIdentifier()), send.datasetIdentifiers());
  }

  @Test
  void testFetchSendsByCreateDate_leaves_unrelated_types_without_dar_or_datasets() {
    generateSendRow(
        createUser(),
        EmailType.DAC_VOTE_REMINDER_DIGEST,
        "2026-10-09",
        Instant.now().minus(1, ChronoUnit.HOURS));

    MailSend send = fetchSendsAroundNow().getFirst();

    assertNull(send.darCode());
    assertEquals(List.of(), send.datasetIdentifiers());
  }

  private DataAccessRequest createDar(User user, Integer collectionId) {
    return createDar(user, collectionId, new Date());
  }

  private DataAccessRequest createDar(User user, Integer collectionId, Date submissionDate) {
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        now,
        submissionDate,
        now,
        new DataAccessRequestData(),
        randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private DataAccessRequest createProgressReport(User user, DataAccessRequest parent) {
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        parent.getId(),
        parent.getCollectionId(),
        referenceId,
        user.getUserId(),
        new DataAccessRequestData(),
        randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private Dataset createDatasetFor(DataAccessRequest dar) {
    Dataset dataset = createDataset();
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDatasetId());
    return dataset;
  }

  private Dataset createDataset() {
    Integer id =
        datasetDAO.insertDataset(
            "Name_" + randomAlphabetic(20),
            new Timestamp(System.currentTimeMillis()),
            createUser().getUserId(),
            null,
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            null);
    return datasetDAO.findDatasetById(id);
  }

  private User createUserNamed(String displayName) {
    User user = createUser();
    userDAO.updateDisplayName(user.getUserId(), displayName);
    return user;
  }

  @Test
  void testFetchSendsByCreateDate_searches_recipients_beyond_the_listed_ones() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(createUserNamed("Alice"), EmailType.NEW_DAR, "DAR-1", first);
    generateSendRow(createUserNamed("Zelda"), EmailType.NEW_DAR, "DAR-1", first);
    generateSendRow(createUserNamed("Bob"), EmailType.NEW_DAR, "DAR-2", first);

    List<MailSend> found = searchSends(MailSendSearch.of("zel", List.of()), 1);

    assertEquals(List.of("DAR-1"), found.stream().map(MailSend::entityReferenceId).toList());
    assertEquals(
        List.of("Alice"),
        found.getFirst().recipients().stream().map(MailSendRecipient::displayName).toList());
  }

  @Test
  void testFetchSendsByCreateDate_searches_dar_codes_duos_ids_and_types() {
    User user = createUser();
    String darCode = uniqueDarCode();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    Dataset dataset = createDatasetFor(createDar(user, collectionId));
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(user, EmailType.NEW_DAR, darCode, first);
    generateSendRow(user, EmailType.DAC_VOTE_REMINDER_DIGEST, "2026-10-09", first.plusSeconds(1));

    assertEquals(1, searchSends(MailSendSearch.of(darCode.toLowerCase(), List.of()), 100).size());
    assertEquals(
        1, searchSends(MailSendSearch.of(dataset.getDatasetIdentifier(), List.of()), 100).size());
    String unpadded = "DUOS-" + Integer.parseInt(dataset.getDatasetIdentifier().substring(5));
    assertEquals(1, searchSends(MailSendSearch.of(unpadded, List.of()), 100).size());
    assertEquals(
        List.of(EmailType.DAC_VOTE_REMINDER_DIGEST.getTypeInt()),
        searchSends(
                MailSendSearch.of(
                    "digest", List.of(EmailType.DAC_VOTE_REMINDER_DIGEST.getTypeInt())),
                100)
            .stream()
            .map(MailSend::emailType)
            .toList());
  }

  @Test
  void testFetchSendsByCreateDate_takes_search_wildcards_literally() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    generateSendRow(createUserNamed("Ada"), EmailType.NEW_DAR, "DAR-1", first);

    assertEquals(List.of(), searchSends(MailSendSearch.of("%", List.of()), 100));
  }

  @Test
  void testFetchSendsByCreateDate_pages_search_results() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    for (int i = 0; i < 3; i++) {
      generateSendRow(
          createUserNamed("Match " + i), EmailType.NEW_DAR, "DAR-" + i, first.plusSeconds(i));
      generateSendRow(
          createUserNamed("Other " + i), EmailType.NEW_CASE, "DAR-" + i, first.plusSeconds(i));
    }
    Date start = Date.from(first.minus(1, ChronoUnit.HOURS));
    Date end = Date.from(Instant.now());
    MailSendSearch search = MailSendSearch.of("match", List.of());

    List<String> pages =
        IntStream.range(0, 3)
            .mapToObj(
                offset ->
                    mailMessageDAO
                        .fetchSendsByCreateDate(start, end, 1, offset, 100, search)
                        .getFirst()
                        .entityReferenceId())
            .toList();

    assertEquals(List.of("DAR-2", "DAR-1", "DAR-0"), pages);
  }

  private List<MailSend> searchSends(MailSendSearch search, int recipientLimit) {
    Instant now = Instant.now();
    return mailMessageDAO.fetchSendsByCreateDate(
        Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now), 100, 0, recipientLimit, search);
  }

  private List<MailSend> fetchSendsAroundNow() {
    Instant now = Instant.now();
    return mailMessageDAO.fetchSendsByCreateDate(
        Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now), 100, 0, 100, MailSendSearch.NONE);
  }

  private MailMessage generateSendRow(
      User user, EmailType emailType, String entityReferenceId, Instant instant) {
    return generateSendRow(user, emailType, entityReferenceId, instant, null);
  }

  private MailMessage generateSendRow(
      User user, EmailType emailType, String entityReferenceId, Instant instant, UUID sendId) {
    return generateSendRow(user, emailType, entityReferenceId, instant, sendId, null);
  }

  private MailMessage generateSendRow(
      User user,
      EmailType emailType,
      String entityReferenceId,
      Instant instant,
      UUID sendId,
      List<Integer> datasetIds) {
    MailMessage savedMessage =
        mailMessageDAO.insert(
            new MailMessageInsert(
                entityReferenceId,
                null,
                user.getUserId(),
                emailType.getTypeInt(),
                Date.from(instant),
                randomAlphanumeric(10),
                randomAlphanumeric(10),
                202,
                sendId,
                datasetIds));
    setCreateDate(savedMessage.emailId(), instant);
    return mailMessageDAO.fetchMessageById(savedMessage.emailId());
  }

  private void setCreateDate(Integer emailId, Instant instant) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "UPDATE email_entity SET create_date = :createDate WHERE email_entity_id = :emailId")
                .bind("createDate", Date.from(instant))
                .bind("emailId", emailId)
                .execute());
  }

  private MailMessage generateMessage(Instant instant) {
    return generateMessage(createUser(), instant);
  }

  private MailMessage generateMessage(User user, Instant instant) {
    MailMessage savedMessage =
        mailMessageDAO.insert(
            new MailMessageInsert(
                randomAlphanumeric(10),
                randomInt(1, 1000),
                user.getUserId(),
                EmailType.COLLECT.getTypeInt(),
                Date.from(instant),
                randomAlphanumeric(10),
                randomAlphanumeric(10),
                randomInt(200, 399),
                null));
    setCreateDate(savedMessage.emailId(), instant);
    return mailMessageDAO.fetchMessageById(savedMessage.emailId());
  }
}
