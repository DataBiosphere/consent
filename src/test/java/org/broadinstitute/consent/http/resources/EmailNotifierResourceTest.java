package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailNotifierResourceTest {

  @Mock private DataAccessRequestService dataAccessRequestService;
  @Mock private EmailService emailService;
  @Mock private AuthUser authUser;

  private EmailNotifierResource resource;

  @BeforeEach
  void setUp() {
    resource = new EmailNotifierResource(dataAccessRequestService, emailService);
  }

  @Test
  void testResourceSuccess() throws Exception {
    doNothing().when(dataAccessRequestService).sendReminderMessage(any());
    try (Response response =
        resource.sendReminderMessage(authUser, String.valueOf(RandomUtils.nextInt(100, 1000)))) {
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
  void testSendDarExpirationNotices() {
    try (Response response = resource.sendDarExpirationNotices(authUser)) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testSendDacVoteDigestNotices() {
    try (Response response = resource.sendDacVoteDigestNotices(authUser)) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testSendDacVoteDigestNotices_Exception() {
    doThrow(new RuntimeException("Some runtime exception"))
        .when(emailService)
        .sendVoteDigestMessages();
    try (Response response = resource.sendDacVoteDigestNotices(authUser)) {
      assertEquals(500, response.getStatus());
    }
  }
}
