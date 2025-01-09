package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.support.DuosTicket;
import org.broadinstitute.consent.http.models.support.TicketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.zendesk.client.v2.model.Request;

/**
 * This test class should be used for manual integration testing only.
 */
class SupportRequestServiceIntegrationTest {

  private SupportRequestService service;

  @BeforeEach
  void setUp() {
    ServicesConfiguration config = new ServicesConfiguration();
    config.setActivateSupportNotifications(true);
    service = new SupportRequestService(config);
  }

  @Disabled
  @Test
  void testPostTicketToSupportWithAttachment() throws Exception {
    String token = service.postAttachmentToSupport("Test Image Content".getBytes());
    DuosTicket ticket = new TicketFactory().createTicket(
        "Test User Name",
        SupportRequestType.QUESTION,
        "test@duos.org",
        "Test Subject",
        "Test Description",
        "Test URL",
        List.of(token)
    );
    Request request = service.postTicketToSupport(ticket);
    assertNotNull(request);
  }

}
