package org.broadinstitute.consent.http.models.support;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;
import org.zendesk.client.v2.model.Comment;
import org.zendesk.client.v2.model.CustomFieldValue;
import org.zendesk.client.v2.model.Ticket;

public class TicketFactory {

  /**
   * Constructs a DuosTicket with the proper structure to request support via Zendesk
   *
   * @param name        The name of the user requesting support
   * @param type        The type of request ("question", "incident", "problem", "task")
   * @param email       The email of the user requesting support
   * @param subject     Subject line of the request
   * @param description Description of the task or question
   * @param url         The origin url of this request
   * @param uploads     Optional list of attachment tokens
   */
  public DuosTicket createTicket(String name, SupportRequestType type, String email, String subject,
      String description, String url, List<String> uploads) {
    if (name == null || email == null) {
      throw new IllegalArgumentException("Name and email of user requesting support is required");
    }
    if (subject == null) {
      throw new IllegalArgumentException("Support ticket subject is required");
    }
    if (description == null) {
      throw new IllegalArgumentException("Support ticket description is required");
    }
    if (type == null) {
      throw new IllegalArgumentException("Support ticket type is required");
    }
    if (url == null) {
      throw new IllegalArgumentException("Support ticket url is required");
    }

    Ticket ticket = new Ticket(
        new Ticket.Requester(name, email),
        subject,
        createComment(description, url, uploads));
    ticket.setCustomFields(createCustomFields(name, type, email, description));
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

  static private List<CustomFieldValue> createCustomFields(String name, SupportRequestType type,
      String email, String description) {
    return List.of(new CustomFieldValue(360012744452L, new String[]{type.getValue()}),
        new CustomFieldValue(360007369412L, new String[]{description}),
        new CustomFieldValue(360012744292L, new String[]{name}),
        new CustomFieldValue(360012782111L, new String[]{email}),
        new CustomFieldValue(360018545031L, new String[]{email}));
  }

}
