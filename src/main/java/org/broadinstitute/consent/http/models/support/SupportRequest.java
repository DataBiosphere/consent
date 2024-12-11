package org.broadinstitute.consent.http.models.support;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;

/**
 * Represents Request object for the Zendesk API
 * <a
 * href="https://developer.zendesk.com/api-reference/ticketing/tickets/ticket-requests/">Requests
 * API Reference</a>
 */
public record SupportRequest(
    SupportRequester requester,
    String subject,
    List<CustomRequestField> customFields,
    SupportRequestComment comment,
    long ticketFormId) {

  public SupportRequest(String name, SupportRequestType type, String email, String subject,
      String description, String url, List<String> uploads) {
    this(new SupportRequester(name, email),
        subject,
        createCustomFields(name, type, email, description),
        new SupportRequestComment(
            description + "\n\n------------------\nSubmitted from: " + url, uploads),
        360000669472L
    );
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
