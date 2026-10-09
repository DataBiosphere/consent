package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.broadinstitute.consent.http.models.mail.MailMessageInsert;
import org.broadinstitute.consent.http.models.mail.MailMessageSummary;
import org.broadinstitute.consent.http.models.mail.MailSend;
import org.broadinstitute.consent.http.models.mail.MailSendRecipient;
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
  void testFetchSummariesByCreateDate() {
    Instant now = Instant.now();
    MailMessage messageToday = generateMessage(now);
    generateMessage(now.minus(2, ChronoUnit.DAYS));

    List<MailMessageSummary> summaries =
        mailMessageDAO.fetchMessageSummariesByCreateDate(
            Date.from(now.minus(1, ChronoUnit.HOURS)),
            Date.from(now.plus(1, ChronoUnit.HOURS)),
            10,
            0);

    assertEquals(
        List.of(
            new MailMessageSummary(
                messageToday.entityReferenceId(),
                messageToday.emailId(),
                messageToday.voteId(),
                messageToday.userId(),
                messageToday.emailType(),
                messageToday.dateSent(),
                messageToday.sendgridStatus(),
                messageToday.createDate())),
        summaries);
  }

  @Test
  void testFetchSummariesByCreateDate_excludes_the_end_instant_in_either_order() {
    Instant end = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant start = end.minus(1, ChronoUnit.HOURS);
    MailMessage inside = generateMessage(end.minus(1, ChronoUnit.MINUTES));
    generateMessage(end);

    List<Integer> forward =
        mailMessageDAO
            .fetchMessageSummariesByCreateDate(Date.from(start), Date.from(end), 10, 0)
            .stream()
            .map(MailMessageSummary::emailId)
            .toList();
    List<Integer> reversed =
        mailMessageDAO
            .fetchMessageSummariesByCreateDate(Date.from(end), Date.from(start), 10, 0)
            .stream()
            .map(MailMessageSummary::emailId)
            .toList();

    assertEquals(List.of(inside.emailId()), forward);
    assertEquals(forward, reversed);
  }

  @Test
  void testFetchSummariesByCreateDate_keeps_null_vote_and_status() {
    Instant now = Instant.now();
    mailMessageDAO.insert(
        new MailMessageInsert(
            randomAlphanumeric(10),
            null,
            createUser().getUserId(),
            EmailType.NEW_DAR.getTypeInt(),
            null,
            randomAlphanumeric(10),
            null,
            null,
            null));

    MailMessageSummary summary =
        mailMessageDAO
            .fetchMessageSummariesByCreateDate(
                Date.from(now.minus(1, ChronoUnit.HOURS)),
                Date.from(now.plus(1, ChronoUnit.HOURS)),
                1,
                0)
            .getFirst();

    assertNull(summary.voteId());
    assertNull(summary.sendgridStatus());
    assertNull(summary.dateSent());
  }

  @Test
  void testFetchSummariesByCreateDate_pages_through_tied_create_dates_by_id() {
    Instant now = Instant.now();
    List<Integer> idsNewestFirst =
        List.of(generateMessage(now), generateMessage(now), generateMessage(now)).stream()
            .map(MailMessage::emailId)
            .sorted(Comparator.reverseOrder())
            .toList();
    Date start = Date.from(now.minus(1, ChronoUnit.HOURS));
    Date end = Date.from(now.plus(1, ChronoUnit.HOURS));

    List<Integer> pagedIds =
        IntStream.range(0, 3)
            .mapToObj(
                offset ->
                    mailMessageDAO
                        .fetchMessageSummariesByCreateDate(start, end, 1, offset)
                        .getFirst()
                        .emailId())
            .toList();

    assertEquals(idsNewestFirst, pagedIds);
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
    generateSendRow(bob, EmailType.NEW_DAR, "DAR-1", first.plus(2, ChronoUnit.MINUTES));

    List<MailSend> sends = fetchSendsAroundNow();

    assertEquals(
        List.of(
            new MailSend(
                earliest.emailId(),
                EmailType.NEW_DAR.getTypeInt(),
                "DAR-1",
                earliest.createDate(),
                3,
                List.of(
                    new MailSendRecipient(alice.getUserId(), "Alice"),
                    new MailSendRecipient(bob.getUserId(), "Bob"),
                    new MailSendRecipient(carol.getUserId(), "Carol")))),
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
                Date.from(first.minus(1, ChronoUnit.HOURS)), Date.from(Instant.now()), 10, 0, 2)
            .getFirst();

    assertEquals(3, send.recipientCount());
    assertEquals(
        List.of(
            new MailSendRecipient(alice.getUserId(), "Alice"),
            new MailSendRecipient(bob.getUserId(), "Bob")),
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
  void testFetchSendsByCreateDate_starts_a_new_send_when_a_recipient_repeats() {
    Instant first = Instant.now().minus(1, ChronoUnit.HOURS);
    User alice = createUser();
    User bob = createUser();
    for (Instant created : List.of(first, first.plus(5, ChronoUnit.MINUTES))) {
      generateSendRow(alice, EmailType.REMINDER, "42", created);
      generateSendRow(bob, EmailType.REMINDER, "42", created);
    }

    List<Integer> recipientCounts =
        fetchSendsAroundNow().stream().map(MailSend::recipientCount).toList();

    assertEquals(List.of(2, 2), recipientCounts);
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
                        .fetchSendsByCreateDate(start, end, 1, offset)
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
        mailMessageDAO.fetchSendsByCreateDate(Date.from(start), Date.from(end), 10, 0).stream()
            .map(MailSend::sendId)
            .toList();
    List<Integer> reversed =
        mailMessageDAO.fetchSendsByCreateDate(Date.from(end), Date.from(start), 10, 0).stream()
            .map(MailSend::sendId)
            .toList();

    assertEquals(List.of(inside.emailId()), forward);
    assertEquals(forward, reversed);
  }

  private User createUserNamed(String displayName) {
    User user = createUser();
    userDAO.updateDisplayName(user.getUserId(), displayName);
    return user;
  }

  private List<MailSend> fetchSendsAroundNow() {
    Instant now = Instant.now();
    return mailMessageDAO.fetchSendsByCreateDate(
        Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now), 100, 0);
  }

  private MailMessage generateSendRow(
      User user, EmailType emailType, String entityReferenceId, Instant instant) {
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
                202));
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
