package org.broadinstitute.consent.http.service;

import com.google.api.client.http.HttpResponse;
import java.util.List;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.broadinstitute.consent.http.models.support.TicketCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.zendesk.client.v2.model.Ticket;

/**
 * This test class should be used for manual integration testing only.
 */
class SupportRequestServiceIntegrationTest {

  private SupportRequestService service;
  private TicketCreator ticketCreator;

  @BeforeEach
  void setUp() {
    ServicesConfiguration config = new ServicesConfiguration();
    config.setActivateSupportNotifications(true);
    service = new SupportRequestService(config);
    ticketCreator = new TicketCreator();
  }


  @Disabled
  @Test
  void testPostZendeskTicket() throws Exception {
    Ticket ticket = ticketCreator.createTicket(
        "Test User Name",
        SupportRequestType.QUESTION,
        "test@duos.org",
        "Test Subject",
        "Test Description",
        "Test URL",
        List.of()
    );
    HttpResponse response = service.postZendeskTicket(ticket);
  }

  @Disabled
  @Test
  void testPostTicketToSupport() throws Exception {
    SupportTicket supportTicket = new SupportTicket(
        "Test User Name",
        SupportRequestType.QUESTION,
        "test@duos.org",
        "Test Subject",
        "Test Description",
        "Test URL",
        List.of()
    );
    service.postTicketToSupport(supportTicket);
  }

}
