package org.broadinstitute.consent.http.models.support;

import java.util.ArrayList;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;

/**
 * Represents Request object for the Zendesk API
 * <a href="https://developer.zendesk.com/api-reference/ticketing/tickets/ticket-requests/">Requests
 * API Reference</a>
 */
public class SupportRequest {

  private SupportRequester requester;
  private String subject;
  private List<CustomRequestField> customFields;
  private SupportRequestComment comment;
  private final long ticketFormId = 360000669472L;

  public SupportRequest(String name, SupportRequestType type, String email, String subject,
      String description, String url, List<String> uploads) {
    this.requester = new SupportRequester(name, email);
    this.subject = subject;
    this.customFields = createCustomFields(name, type, email, description);
    this.comment = new SupportRequestComment(
        description + "\n\n------------------\nSubmitted from: " + url, uploads);
  }

  public SupportRequester getRequester() {
    return requester;
  }

  public void setRequester(SupportRequester requester) {
    this.requester = requester;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public List<CustomRequestField> getCustomFields() {
    return customFields;
  }

  public void setCustomFields(List<CustomRequestField> customFields) {
    this.customFields = customFields;
  }

  public SupportRequestComment getComment() {
    return comment;
  }

  public void setComment(SupportRequestComment comment) {
    this.comment = comment;
  }

  public long getTicketFormId() {
    return ticketFormId;
  }

  static private List<CustomRequestField> createCustomFields(String name, SupportRequestType type,
      String email, String description) {
    return List.of(new CustomRequestField(360012744452L, type.getValue()),
        new CustomRequestField(360007369412L, description),
        new CustomRequestField(360012744292L, name),
        new CustomRequestField(360012782111L, email),
        new CustomRequestField(360018545031L, email));
  }
}
