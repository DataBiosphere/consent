package org.broadinstitute.consent.http.models.support;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;

/**
 * Represents ticket to request support required by the Zendesk API
 * <a
 * href="https://developer.zendesk.com/api-reference/ticketing/tickets/ticket-requests/">Requests
 * API Reference</a>
 * <p>
 * This is the base object that we use to construct a SupportRequest object that is eventually
 * serialized and sent to Zendesk in json format.
 */
public class SupportTicket {

  private SupportRequest request;

  /**
   * Constructs a ticket with the proper structure to request support via Zendesk
   *
   * @param name        The name of the user requesting support
   * @param type        The type of request ("question", "incident", "problem", "task")
   * @param email       The email of the user requesting support
   * @param subject     Subject line of the request
   * @param description Description of the task or question
   * @param url         The origin url of this request
   * @param uploads     Optional list of attachment tokens
   */
  public SupportTicket(String name, SupportRequestType type, String email, String subject,
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

    this.request = new SupportRequest(name, type, email, subject, description, url, uploads);
  }

  public SupportRequest getRequest() {
    return request;
  }

  public void setRequest(SupportRequest request) {
    this.request = request;
  }

  public String toString() {
    //Using GsonBuilder directly to convert ticket to json since GsonFactory does not allow custom FieldNamingPolicy
    return new GsonBuilder()
        .setPrettyPrinting()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()
        .toJson(this);
  }
}
