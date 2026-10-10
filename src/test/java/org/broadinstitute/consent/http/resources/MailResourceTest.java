package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.broadinstitute.consent.http.models.mail.MailMessageSummary;
import org.broadinstitute.consent.http.models.mail.MailSend;
import org.broadinstitute.consent.http.models.mail.MailSendRecipient;
import org.broadinstitute.consent.http.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MailResourceTest extends AbstractTestHelper {

  @Mock private EmailService emailService;
  private final AuthUser authUser = new AuthUser("test@test.com");
  private final User user = new User(1, authUser.getEmail(), "Display Name", new Date());
  private final DuosUser duosUser = new DuosUser(authUser, user);

  private MailResource mailResource;

  private void initResource() {
    mailResource = new MailResource(emailService);
  }

  @Test
  void test_MailResource() {
    initResource();
    when(emailService.fetchEmailMessagesByType(any(), any(), any()))
        .thenReturn(generateMailMessageList());
    Response response = mailResource.getEmailByType(duosUser, EmailType.COLLECT, null, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResourceEmptyListResponse() {
    initResource();
    when(emailService.fetchEmailMessagesByType(any(), any(), any())).thenReturn(new ArrayList<>());
    Response response = mailResource.getEmailByType(duosUser, EmailType.COLLECT, null, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResourceByUser() {
    initResource();
    when(emailService.fetchEmailMessagesByUserId(any(), any(), any()))
        .thenReturn(generateMailMessageList());
    Response response = mailResource.getEmailByUser(duosUser, 1, null, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResourceByUserEmptyListResponse() {
    initResource();
    when(emailService.fetchEmailMessagesByUserId(any(), any(), any()))
        .thenReturn(new ArrayList<>());
    Response response = mailResource.getEmailByUser(duosUser, 1, null, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResource_date_range_EmptyListResponse() {
    initResource();
    when(emailService.fetchEmailMessagesByCreateDate(any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());
    Response response =
        mailResource.getEmailByDateRange(duosUser, "05/11/2021", "05/11/2022", null, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResource_date_range_ListResponse() {
    initResource();
    when(emailService.fetchEmailMessagesByCreateDate(any(), any(), any(), any()))
        .thenReturn(generateMailMessageList());
    Response response =
        mailResource.getEmailByDateRange(duosUser, "05/11/2021", "05/11/2022", null, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResource_date_range_invalid_limit() {
    initResource();
    assertThrows(
        BadRequestException.class,
        () -> {
          mailResource.getEmailByDateRange(duosUser, "05/11/2021", "05/11/2022", -5, null);
        });
  }

  @Test
  void test_MailResource_date_range_invalid_offset() {
    initResource();
    assertThrows(
        BadRequestException.class,
        () -> {
          mailResource.getEmailByDateRange(duosUser, "05/11/2021", "05/11/2022", null, -1);
        });
  }

  @Test
  void test_MailResource_invalid_start_date() {
    initResource();
    Response response =
        mailResource.getEmailByDateRange(duosUser, "55/11/2021", "05/11/2022", null, null);
    assertEquals(400, response.getStatus());
  }

  @Test
  void test_MailResource_invalid_end_date() {
    initResource();
    Response response =
        mailResource.getEmailByDateRange(duosUser, "05/11/2021", "65/98/20229", null, null);
    assertEquals(400, response.getStatus());
  }

  @Test
  void test_MailResource_summary_ListResponse() throws Exception {
    initResource();
    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
    List<MailMessageSummary> summaries =
        List.of(new MailMessageSummary("DAR-1", 1, null, 2, 4, null, 202, new Date()));
    when(emailService.fetchEmailMessageSummariesByCreateDate(
            df.parse("05/11/2021"), df.parse("05/11/2022"), 50, 10))
        .thenReturn(summaries);

    Response response =
        mailResource.getEmailSummaryByDateRange(duosUser, "05/11/2021", "05/11/2022", 50, 10);

    assertEquals(200, response.getStatus());
    assertEquals(summaries, response.getEntity());
  }

  @Test
  void test_MailResource_summary_without_end_runs_through_today() throws Exception {
    initResource();
    Date tomorrowStart =
        Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(emailService.fetchEmailMessageSummariesByCreateDate(
            new SimpleDateFormat("MM/dd/yyyy").parse("05/11/2021"), tomorrowStart, 20, 0))
        .thenReturn(List.of());

    Response response =
        mailResource.getEmailSummaryByDateRange(duosUser, "05/11/2021", null, 20, 0);

    assertEquals(200, response.getStatus());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"55/11/2021", "05/11/2021garbage"})
  void test_MailResource_summary_unusable_start_date(String start) {
    initResource();
    Response response =
        mailResource.getEmailSummaryByDateRange(duosUser, start, "05/11/2022", null, null);
    assertEquals(400, response.getStatus());
    verifyNoInteractions(emailService);
  }

  @Test
  void test_MailResource_summary_limit_above_cap() {
    initResource();
    int limit = MailResource.MAX_PAGE_LIMIT + 1;
    assertThrows(
        BadRequestException.class,
        () -> mailResource.getEmailSummaryByDateRange(duosUser, "05/11/2021", null, limit, null));
  }

  @Test
  void test_MailResource_summary_invalid_limit() {
    initResource();
    assertThrows(
        BadRequestException.class,
        () -> mailResource.getEmailSummaryByDateRange(duosUser, "05/11/2021", null, -5, null));
  }

  private List<MailMessage> generateMailMessageList() {
    List<MailMessage> messageList = new ArrayList<>();
    EnumSet.allOf(EmailType.class)
        .forEach(t -> messageList.add(generateMailMessage(t.getTypeInt())));
    return messageList;
  }

  private MailMessage generateMailMessage(int emailType) {
    return new MailMessage(
        randomAlphanumeric(10),
        nextInt(),
        nextInt(),
        nextInt(),
        emailType,
        new Date(),
        randomAlphanumeric(10),
        randomAlphanumeric(10),
        nextInt(),
        new Date());
  }

  @Test
  void test_MailResource_sends_ListResponse() throws Exception {
    initResource();
    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
    List<MailSend> sends =
        List.of(
            new MailSend(
                1, 34, "2026-10-09", new Date(), 1, List.of(new MailSendRecipient(2, "A", true))));
    when(emailService.fetchEmailSendsByCreateDate(
            df.parse("05/11/2021"), df.parse("05/11/2022"), 50, 10))
        .thenReturn(sends);

    Response response =
        mailResource.getEmailSendsByDateRange(duosUser, "05/11/2021", "05/11/2022", 50, 10);

    assertEquals(200, response.getStatus());
    assertEquals(sends, response.getEntity());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"55/11/2021", "05/11/2021garbage"})
  void test_MailResource_sends_unusable_start_date(String start) {
    initResource();
    Response response =
        mailResource.getEmailSendsByDateRange(duosUser, start, "05/11/2022", null, null);
    assertEquals(400, response.getStatus());
    verifyNoInteractions(emailService);
  }

  @Test
  void test_MailResource_sends_limit_above_cap() {
    initResource();
    int limit = MailResource.MAX_PAGE_LIMIT + 1;
    assertThrows(
        BadRequestException.class,
        () -> mailResource.getEmailSendsByDateRange(duosUser, "05/11/2021", null, limit, null));
  }

  @Test
  void test_MailResource_sends_without_end_runs_through_today() throws Exception {
    initResource();
    Date tomorrowStart =
        Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    when(emailService.fetchEmailSendsByCreateDate(
            new SimpleDateFormat("MM/dd/yyyy").parse("05/11/2021"), tomorrowStart, 20, 0))
        .thenReturn(List.of());

    Response response = mailResource.getEmailSendsByDateRange(duosUser, "05/11/2021", null, 20, 0);

    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResource_sends_negative_offset() {
    initResource();
    assertThrows(
        BadRequestException.class,
        () -> mailResource.getEmailSendsByDateRange(duosUser, "05/11/2021", null, 20, -1));
  }
}
