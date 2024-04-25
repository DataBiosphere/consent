package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.broadinstitute.consent.http.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MailResourceTest {

  @Mock
  private EmailService emailService;
  private final AuthUser authUser = new AuthUser("test@test.com");

  private MailResource mailResource;

  private void initResource() {
    mailResource = new MailResource(emailService);
  }

  @Test
  void test_MailResource() {
    initResource();
    when(emailService.fetchEmailMessagesByType(any(), any(), any())).thenReturn(
        generateMailMessageList());
    Response response = mailResource.getEmailByType(authUser, EmailType.COLLECT, null, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResourceEmptyListResponse() {
    initResource();
    when(emailService.fetchEmailMessagesByType(any(), any(), any())).thenReturn(new ArrayList<>());
    Response response = mailResource.getEmailByType(authUser, EmailType.COLLECT, null, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResource_date_range_EmptyListResponse() {
    initResource();
    when(emailService.fetchEmailMessagesByCreateDate(any(), any(), any(), any())).thenReturn(
        new ArrayList<>());
    Response response = mailResource.getEmailByDateRange(authUser, "05/11/2021", "05/11/2022", null,
        null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResource_date_range_ListResponse() {
    initResource();
    when(emailService.fetchEmailMessagesByCreateDate(any(), any(), any(), any())).thenReturn(
        generateMailMessageList());
    Response response = mailResource.getEmailByDateRange(authUser, "05/11/2021", "05/11/2022", null,
        null);
    assertEquals(200, response.getStatus());
  }

  @Test
  void test_MailResource_date_range_invalid_limit() {
    initResource();
    assertThrows(BadRequestException.class, () -> {
      mailResource.getEmailByDateRange(authUser, "05/11/2021", "05/11/2022", -5, null);
    });
  }

  @Test
  void test_MailResource_date_range_invalid_offset() {
    initResource();
    assertThrows(BadRequestException.class, () -> {
      mailResource.getEmailByDateRange(authUser, "05/11/2021", "05/11/2022", null, -1);
    });
  }

  @Test
  void test_MailResource_invalid_start_date() {
    initResource();
    Response response = mailResource.getEmailByDateRange(authUser, "55/11/2021", "05/11/2022", null,
        null);
    assertEquals(400, response.getStatus());
  }

  @Test
  void test_MailResource_invalid_end_date() {
    initResource();
    Response response = mailResource.getEmailByDateRange(authUser, "05/11/2021", "65/98/20229",
        null, null);
    assertEquals(400, response.getStatus());
  }

  private List<MailMessage> generateMailMessageList() {
    List<MailMessage> messageList = new ArrayList<>();
    EnumSet.allOf(EmailType.class).forEach(t ->
        messageList.add(generateMailMessage(t.toString())));
    return messageList;
  }

  private MailMessage generateMailMessage(String emailType) {
    return new MailMessage(
        RandomUtils.nextInt(),
        RandomUtils.nextInt(),
        RandomUtils.nextInt(),
        emailType,
        new Date(),
        RandomStringUtils.randomAlphanumeric(10),
        RandomStringUtils.randomAlphanumeric(10),
        RandomUtils.nextInt(),
        new Date()
    );
  }
}
