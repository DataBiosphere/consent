package org.broadinstitute.consent.http.models.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.zendesk.client.v2.model.Request;
import org.zendesk.client.v2.model.Ticket;

public class TicketFactory implements ConsentLogger {

  private static final String REQUEST = "request";

  public TicketFactory() {
    // public constructor to allow instantiation for instance methods
  }

  /**
   * Generate the created Request object from the Zendesk request response content.
   *
   * @param response The response content from the Zendesk Request API
   * @return Parsed request.
   */
  public Request parseRequestResponse(String response) {
    Gson gson = GsonUtil.getInstance();
    JsonObject obj;
    try {
      obj = gson.fromJson(response, JsonObject.class);
    } catch (JsonParseException e) {
      logException("Invalid Zendesk response: %s".formatted(response), e);
      throw e;
    }
    if (obj == null || !obj.has(REQUEST) || !obj.get(REQUEST).isJsonObject()) {
      logWarn("Invalid Zendesk response: %s".formatted(response));
      throw new IllegalStateException(
          "Invalid Zendesk response: 'request' field is missing or not a JSON object.");
    }
    JsonObject request = obj.get(REQUEST).getAsJsonObject();
    return gson.fromJson(request, Request.class);
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
