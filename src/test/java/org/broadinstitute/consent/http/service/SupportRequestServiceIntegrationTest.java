package org.broadinstitute.consent.http.service;

import com.google.api.client.http.HttpResponse;
import java.util.List;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.support.DuosTicket;
import org.broadinstitute.consent.http.models.support.TicketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
    System.out.println(token);
    DuosTicket ticket = new TicketFactory().createTicket(
        "Test User Name",
        SupportRequestType.QUESTION,
        "test@duos.org",
        "Test Subject",
        "Test Description",
        "Test URL",
        List.of(token)
    );
    HttpResponse response = service.postTicketToSupport(ticket);
  }

}
