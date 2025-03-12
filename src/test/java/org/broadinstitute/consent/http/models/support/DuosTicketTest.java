package org.broadinstitute.consent.http.models.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.junit.jupiter.api.Test;

class DuosTicketTest {

  @Test
  void toStringSerialization() {
    TicketFields ticketFields = new TicketFields(
        "Test User",
        SupportRequestType.QUESTION,
        "test.user@example.com",
        "Test Subject",
        "Test Description",
        "https://example.com",
        List.of("token1", "token2"));
    DuosTicket ticket = TicketFactory.createTicket(ticketFields);
    assertEquals("""
            {"request":{"subject":"Test Subject","requester":{"name":"Test User","email":"test.user@example.com"},"comment":{"type":"Comment","body":"Test Description\\n\\n------------------\\nSubmitted from: https://example.com","uploads":["token1","token2"]},"custom_fields":[{"id":360012744452,"value":"QUESTION"},{"id":360007369412,"value":"Test Description"},{"id":360012744292,"value":"Test User"},{"id":360012782111,"value":"test.user@example.com"},{"id":360018545031,"value":"test.user@example.com"}],"has_incidents":false,"ticket_form_id":360000669472}}""",
        ticket.toString());

  }
}