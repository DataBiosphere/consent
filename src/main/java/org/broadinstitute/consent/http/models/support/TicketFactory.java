package org.broadinstitute.consent.http.models.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.zendesk.client.v2.model.Comment;
import org.zendesk.client.v2.model.CustomFieldValue;
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
   * @param ticketFields  TicketFields
   */
  public static DuosTicket createTicket(TicketFields ticketFields) {
    ticketFields.validate();
    Ticket ticket = new Ticket(
        new Ticket.Requester(ticketFields.name(), ticketFields.email()),
        ticketFields.subject(),
        createComment(ticketFields.description(), ticketFields.url(), ticketFields.uploads()));
    ticket.setCustomFields(createCustomFields(ticketFields.name(), ticketFields.type(), ticketFields.email(), ticketFields.description()));
    // This value specifies tickets as belonging to the DUOS group defined in Zendesk
    ticket.setTicketFormId(360000669472L);
    return new DuosTicket(ticket);
  }

  static private Comment createComment(String description, String url, List<String> uploads) {
    Comment comment = new Comment();
    comment.setBody(description + "\n\n------------------\nSubmitted from: " + url);
    if (uploads != null && !uploads.isEmpty()) {
      comment.setUploads(uploads);
    }
    return comment;
  }

  /**
   * Custom fields represent a Zendesk ID that corresponds to a component of the created ticket.
   * These fields have IDs that are defined in the Zendesk administrative interface.
   *
   * @param name Name of user
   * @param type SupportRequestType Type of request, i.e. Question, Incident, etc.
   * @param email User's email address
   * @param description The contents of the support request
   * @return List of Custom Fields that are required by Zendesk
   */
  static private List<CustomFieldValue> createCustomFields(String name, SupportRequestType type,
      String email, String description) {
    return List.of(new CustomFieldValue(360012744452L, new String[]{type.name()}),
        new CustomFieldValue(360007369412L, new String[]{description}),
        new CustomFieldValue(360012744292L, new String[]{name}),
        new CustomFieldValue(360012782111L, new String[]{email}),
        new CustomFieldValue(360018545031L, new String[]{email}));
  }

}
