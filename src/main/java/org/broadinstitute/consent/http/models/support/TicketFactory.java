package org.broadinstitute.consent.http.models.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.zendesk.client.v2.model.Ticket;

public class TicketFactory implements ConsentLogger {

  private static final String REQUEST = "request";
  private static final String SUSPENDED_TICKET = "suspended_ticket";

  public TicketFactory() {
    // public constructor to allow instantiation for instance methods
  }

  /**
   * Validate and return the response from the Zendesk request.
   *
   * @param response The response content from the Zendesk Request API
   * @return Parsed Json.
   */
  public JsonObject parseZendeskResponse(String response) {
    Gson gson = GsonUtil.getInstance();
    JsonObject obj;
    try {
      obj = gson.fromJson(response, JsonObject.class);
    } catch (JsonParseException e) {
      logException("Invalid Zendesk response: %s".formatted(response), e);
      throw e;
    }
    if (!(isVerifiedZendeskRequest(obj) || isSuspendedZendeskRequest(obj))) {
      logWarn("Invalid Zendesk response: %s".formatted(response));
      throw new IllegalStateException(
          "Invalid Zendesk response: 'request' field is missing or not a JSON object.");
    }

    return obj;
  }

  private boolean isVerifiedZendeskRequest(JsonObject obj) {
    return obj != null && obj.has(REQUEST) && obj.get(REQUEST).isJsonObject();
  }

  private boolean isSuspendedZendeskRequest(JsonObject obj) {
    return obj != null && obj.has(SUSPENDED_TICKET) && obj.get(SUSPENDED_TICKET).isJsonObject();
  }

  /**
   * Constructs a DuosTicket with the proper structure to request support via Zendesk
   *
   * @param ticketFields TicketFields
   */
  public static DuosTicket createTicket(TicketFields ticketFields) {
    ticketFields.validate();
    Ticket ticket = ticketFields.toTicket();
    return new DuosTicket(ticket);
  }
}
