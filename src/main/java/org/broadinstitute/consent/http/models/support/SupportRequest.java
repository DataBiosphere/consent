package org.broadinstitute.consent.http.models.support;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.zendesk.client.v2.model.Comment;
import org.zendesk.client.v2.model.CustomFieldValue;
import org.zendesk.client.v2.model.Ticket;

/**
 * Represents Request object for the Zendesk API
 * <a
 * href="https://developer.zendesk.com/api-reference/ticketing/tickets/ticket-requests/">Requests
 * API Reference</a>
 */
public record SupportRequest(
    Ticket.Requester requester,
    String subject,
    List<CustomFieldValue> customFields,
    Comment comment,
    long ticketFormId) {

  public SupportRequest(String name, SupportRequestType type, String email, String subject,
      String description, String url, List<String> uploads) {

    this(new Ticket.Requester(name, email),
        subject,
        createCustomFields(name, type, email, description),
        createComment(description, url, uploads),
        360000669472L
    );
  }

  public Ticket createTicket() {
    Ticket ticket = new Ticket(
        this.requester(),
        this.subject(),
        this.comment());
    ticket.setCustomFields(this.customFields());
    ticket.setTicketFormId(this.ticketFormId());
    return ticket;
  }

  static private Comment createComment(String description, String url, List<String> uploads) {
    Comment comment = new Comment();
    comment.setBody(description + "\n\n------------------\nSubmitted from: " + url);
    if (uploads != null && !uploads.isEmpty()) {
      comment.setUploads(uploads);
      }
    return comment;
  }

  static private List<CustomFieldValue> createCustomFields(String name, SupportRequestType type,
      String email, String description) {
    return List.of(new CustomFieldValue(360012744452L, new String[] {type.getValue()}),
        new CustomFieldValue(360007369412L, new String[]{description}),
        new CustomFieldValue(360012744292L, new String[]{name}),
        new CustomFieldValue(360012782111L, new String[]{email}),
        new CustomFieldValue(360018545031L, new String[]{email}));
  }
}
