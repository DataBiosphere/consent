package org.broadinstitute.consent.http.models.support;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.zendesk.client.v2.model.Comment;
import org.zendesk.client.v2.model.CustomFieldValue;
import org.zendesk.client.v2.model.Ticket;

public record TicketFields(
    String name,
    SupportRequestType type,
    String email,
    String subject,
    String description,
    String url,
    List<String> uploads) {

  private static void validate(boolean condition, String prefix) {
    if (condition) {
      throw new IllegalArgumentException(prefix + " is required");
    }
  }

  public void validate() {
    validate(name == null || email == null, "Name and email of user requesting support");
    validate(subject == null, "Subject");
    validate(description == null, "Description");
    validate(type == null, "Type");
    validate(url == null, "URL");
  }

  Ticket toTicket(SupportRequester requester) {
    Ticket ticket =
        new Ticket(new Ticket.Requester(name, email), subject, createComment(requester));
    ticket.setCustomFields(createCustomFields());
    // This value specifies tickets as belonging to the DUOS group defined in Zendesk
    ticket.setTicketFormId(360000669472L);
    return ticket;
  }

  /**
   * The note on who sent the request comes first, before anything the caller wrote, so a line in
   * the description or URL can't pass for it. The URL is one line.
   */
  private Comment createComment(SupportRequester requester) {
    Comment comment = new Comment();
    comment.setBody(
        requester.note()
            + "\n\n"
            + description
            + "\n\n------------------\nSubmitted from: "
            + url.replaceAll("[\\r\\n]+", " "));
    if (uploads != null && !uploads.isEmpty()) {
      comment.setUploads(uploads);
    }
    return comment;
  }

  /**
   * Custom fields consist of predefined long values corresponding to field types in the Zendesk UI
   *
   * @return List<CustomFieldValue> List of custom fields
   */
  private List<CustomFieldValue> createCustomFields() {
    return List.of(
        new CustomFieldValue(360012744452L, new String[] {type.name()}),
        new CustomFieldValue(360007369412L, new String[] {description}),
        new CustomFieldValue(360012744292L, new String[] {name}),
        new CustomFieldValue(360012782111L, new String[] {email}),
        new CustomFieldValue(360018545031L, new String[] {email}));
  }
}
