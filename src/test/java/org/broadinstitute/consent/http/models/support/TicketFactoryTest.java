package org.broadinstitute.consent.http.models.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
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

  private static final String SANITIZED_SUSPENDED_TICKET_RESPONSE =
      """
          {
            "suspended_ticket": {
              "url": "https://example.zendesk.com/api/v2/suspended_tickets/5555.json",
              "id": 5555,
              "author": {
                "id": 987654321,
                "name": "Support User",
                "email": "user@example.org"
              },
              "subject": "DUOS: User Request",
              "content": "User (2222, user@example.org) submitted a support request.",
              "cause": "Anonymous request",
              "cause_id": 13,
              "error_messages": null,
              "message_id": null,
              "ticket_id": null,
              "created_at": "2026-05-15T16:58:43Z",
              "updated_at": "2026-05-15T16:58:43Z",
              "via": {
                "channel": "api",
                "source": {
                  "from": {},
                  "to": {},
                  "rel": null
                }
              },
              "attachments": [],
              "recipient": null,
              "brand_id": 1234
            }
          }
          """;

  @Test
  void testParseZendeskResponse() {
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
    JsonObject parsedRequest = new TicketFactory().parseZendeskResponse(validResponse);
    assertNotNull(parsedRequest);
    assertTrue(parsedRequest.has("request"));
    assertEquals(
        request.getId(), parsedRequest.get("request").getAsJsonObject().get("id").getAsLong());
    assertEquals(
        request.getSubject(),
        parsedRequest.get("request").getAsJsonObject().get("subject").getAsString());
  }

  @Test
  void testParseZendeskResponse_SuspendedTicket() {
    JsonObject parsedResponse =
        new TicketFactory().parseZendeskResponse(SANITIZED_SUSPENDED_TICKET_RESPONSE);

    assertNotNull(parsedResponse);
    assertTrue(parsedResponse.has("suspended_ticket"));
    assertEquals(
        5555, parsedResponse.get("suspended_ticket").getAsJsonObject().get("id").getAsLong());
  }

  @Test
  void testParseZendeskResponse_InvalidRequestButValidSuspendedTicket() {
    String responseWithInvalidRequestAndValidSuspendedTicket =
        """
            {
              "request": "not-an-object",
              "suspended_ticket": {
                "id": 50425014126235
              }
            }
            """;

    JsonObject parsedResponse =
        new TicketFactory().parseZendeskResponse(responseWithInvalidRequestAndValidSuspendedTicket);

    assertNotNull(parsedResponse);
    assertTrue(parsedResponse.has("suspended_ticket"));
  }

  @Test
  void testParseZendeskResponse_InvalidJson() {
    String invalidResponse = "invalid json";
    assertThrows(
        JsonSyntaxException.class, () -> new TicketFactory().parseZendeskResponse(invalidResponse));
  }

  @Test
  void testParseRequestResponse_MissingZendesk() {
    String missingRequestResponse = "{}";
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> new TicketFactory().parseZendeskResponse(missingRequestResponse));
    assertTrue(e.getMessage().startsWith(INVALID_RESPONSE_MESSAGE));
  }

  @Test
  void testParseRequestResponse_NullZendesk() {
    String nullRequestResponse =
        """
                {
                  "request": null
                }
                """;
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> new TicketFactory().parseZendeskResponse(nullRequestResponse));
    assertTrue(e.getMessage().startsWith(INVALID_RESPONSE_MESSAGE));
  }

  @Test
  void testParseRequestResponse_ZendeskNotAnObject() {
    String notAnObjectResponse =
        """
                {
                  "request": "I am a string"
                }
                """;
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> new TicketFactory().parseZendeskResponse(notAnObjectResponse));
    assertTrue(e.getMessage().startsWith(INVALID_RESPONSE_MESSAGE));
  }

  @Test
  void testParseZendeskResponse_SuspendedTicketNull() {
    String nullSuspendedTicketResponse =
        """
            {
              "suspended_ticket": null
            }
            """;
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> new TicketFactory().parseZendeskResponse(nullSuspendedTicketResponse));
    assertTrue(e.getMessage().startsWith(INVALID_RESPONSE_MESSAGE));
  }

  @Test
  void testParseZendeskResponse_SuspendedTicketNotAnObject() {
    String suspendedTicketNotAnObjectResponse =
        """
            {
              "suspended_ticket": "I am a string"
            }
            """;
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class,
            () -> new TicketFactory().parseZendeskResponse(suspendedTicketNotAnObjectResponse));
    assertTrue(e.getMessage().startsWith(INVALID_RESPONSE_MESSAGE));
  }

  @Test
  void testParseZendeskResponse_NullInput() {
    String nullInput = null;
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class, () -> new TicketFactory().parseZendeskResponse(nullInput));
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
