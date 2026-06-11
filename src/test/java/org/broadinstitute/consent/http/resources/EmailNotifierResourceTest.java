package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import jakarta.ws.rs.core.Response;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailNotifierResourceTest extends AbstractTestHelper {

  @Mock private DataAccessRequestService dataAccessRequestService;
  @Mock private EmailService emailService;
  @Mock private AuthUser authUser;

  private EmailNotifierResource resource;

  @BeforeEach
  void setUp() {
    resource =
        new EmailNotifierResource(
            dataAccessRequestService, emailService, Executors.newVirtualThreadPerTaskExecutor());
  }

  @Test
  void testResourceSuccess() throws Exception {
    doNothing().when(dataAccessRequestService).sendReminderMessage(any());
    try (Response response =
        resource.sendReminderMessage(authUser, String.valueOf(randomInt(100, 1000)))) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testResourceFailure() {
    try (Response response = resource.sendReminderMessage(authUser, "invalidVoteId")) {
      assertEquals(500, response.getStatus());
    }
  }

  @Test
  void testSendDailyMessages() {
    try (Response response = resource.sendDailyMessages(authUser)) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testSendDailyMessages_VoteDigestMessagesThrows() throws InterruptedException {
    doNothing().when(emailService).sendNewDatasetInDUOSNotifications();
    doNothing().when(dataAccessRequestService).sendExpirationNotices();
    doThrow(new RuntimeException("Exception")).when(emailService).sendVoteDigestMessages();
    try (Response response = resource.sendDailyMessages(authUser)) {
      resource.executor.shutdown();
      assertTrue(resource.executor.awaitTermination(1, TimeUnit.SECONDS));
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testSendDailyMessages_NewDatasetNotificationsThrows() throws InterruptedException {
    doThrow(new RuntimeException("Exception"))
        .when(emailService)
        .sendNewDatasetInDUOSNotifications();
    doNothing().when(dataAccessRequestService).sendExpirationNotices();
    doNothing().when(emailService).sendVoteDigestMessages();
    try (Response response = resource.sendDailyMessages(authUser)) {
      resource.executor.shutdown();
      assertTrue(resource.executor.awaitTermination(1, TimeUnit.SECONDS));
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testSendDailyMessages_SendExpirationsThrows() throws InterruptedException {
    doNothing().when(emailService).sendNewDatasetInDUOSNotifications();
    doThrow(new RuntimeException("Exception"))
        .when(dataAccessRequestService)
        .sendExpirationNotices();
    try (Response response = resource.sendDailyMessages(authUser)) {
      resource.executor.shutdown();
      assertTrue(resource.executor.awaitTermination(1, TimeUnit.SECONDS));
      assertEquals(200, response.getStatus());
    }
  }
}
