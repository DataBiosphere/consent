package org.broadinstitute.consent.http.models.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonSyntaxException;
import java.util.Collections;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zendesk.client.v2.model.Request;
import org.zendesk.client.v2.model.Ticket;

@SuppressWarnings("java:S5778")
@ExtendWith(MockitoExtension.class)
class TicketFactoryTest {

  public static final String INVALID_RESPONSE_MESSAGE =
      "Invalid Zendesk response: 'request' field is missing or not a JSON object";

  @Test
  void testParseRequestResponse() {
    Request request = new Request();
    request.setId(12345L);
    request.setSubject("Test Subject");
    String validResponse =
        String.format(
            """
                {
                  "request": %s
                }
                """,
            GsonUtil.getInstance().toJson(request));
    Request parsedRequest = new TicketFactory().parseRequestResponse(validResponse);
    assertNotNull(parsedRequest);
    assertEquals(request.getId(), parsedRequest.getId());
    assertEquals(request.getSubject(), parsedRequest.getSubject());
  }

  @Test
  void testParseRequestResponse_InvalidJson() {
    String invalidResponse = "invalid json";
    assertThrows(
        JsonSyntaxException.class, () -> new TicketFactory().parseRequestResponse(invalidResponse));
  }

  @Test
  void testParseRequestResponse_MissingRequest() {
    String missingRequestResponse = "{}";
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> new TicketFactory().parseRequestResponse(missingRequestResponse));
    assertTrue(e.getMessage().startsWith(INVALID_RESPONSE_MESSAGE));
  }

  @Test
  void testParseRequestResponse_NullRequest() {
    String nullRequestResponse =
        """
                {
                  "request": null
                }
                """;
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> new TicketFactory().parseRequestResponse(nullRequestResponse));
    assertTrue(e.getMessage().startsWith(INVALID_RESPONSE_MESSAGE));
  }

  @Test
  void testParseRequestResponse_RequestNotAnObject() {
    String notAnObjectResponse =
        """
                {
                  "request": "I am a string"
                }
                """;
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> new TicketFactory().parseRequestResponse(notAnObjectResponse));
    assertTrue(e.getMessage().startsWith(INVALID_RESPONSE_MESSAGE));
  }

  @Test
  void testParseRequestResponse_NullInput() {
    String nullInput = null;
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class, () -> new TicketFactory().parseRequestResponse(nullInput));
    assertTrue(e.getMessage().startsWith(INVALID_RESPONSE_MESSAGE));
  }

  @Test
  void testCreateTicket() {
    TicketFields fields =
        new TicketFields(
            "name",
            SupportRequestType.TASK,
            "email@email.com",
            "subject",
            "description",
            "url",
            Collections.emptyList());
    DuosTicket duosTicket = TicketFactory.createTicket(fields);
    assertNotNull(duosTicket);
    Ticket ticket = duosTicket.request;
    assertNotNull(ticket);
    assertEquals(fields.subject(), ticket.getSubject());
    assertEquals(fields.name(), ticket.getRequester().getName());
    assertEquals(fields.email(), ticket.getRequester().getEmail());
  }

  @Test
  void testCreateTicket_Validation() {
    TicketFields fields =
        new TicketFields(
            null,
            SupportRequestType.TASK,
            "email@email.com",
            "subject",
            "description",
            "url",
            Collections.emptyList());
    assertThrows(IllegalArgumentException.class, () -> TicketFactory.createTicket(fields));
  }
}
