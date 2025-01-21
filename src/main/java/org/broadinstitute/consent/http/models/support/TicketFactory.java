package org.broadinstitute.consent.http.models.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.zendesk.client.v2.model.Request;
import org.zendesk.client.v2.model.Ticket;

public class TicketFactory {

  /**
   * Generate the created Request object from the Zendesk request response content.
   *
   * @param response The response content from the Zendesk Request API
   * @return Parsed request.
   */
  public static Request parseRequestResponse(String response) {
    Gson gson = GsonUtil.getInstance();
    JsonObject obj = gson.fromJson(response, JsonObject.class);
    JsonObject request = obj.get("request").getAsJsonObject();
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
