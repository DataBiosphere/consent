package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailNotifierResourceTest {

  @Mock
  private EmailService emailService;

  private EmailNotifierResource resource;

  @BeforeEach
  void setUp() throws Exception {
    resource = new EmailNotifierResource(emailService);
  }

  @Test
  void testResourceSuccess() throws Exception {
    doNothing().when(emailService).sendReminderMessage(any());
    Response response = resource.sendReminderMessage(
        String.valueOf(RandomUtils.nextInt(100, 1000)));
    assertEquals(200, response.getStatus());
  }

  @Test
  void testResourceFailure() {
    Response response = resource.sendReminderMessage("invalidVoteId");
    assertEquals(500, response.getStatus());
  }

}
