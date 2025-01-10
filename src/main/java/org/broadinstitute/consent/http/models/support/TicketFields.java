package org.broadinstitute.consent.http.models.support;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.SupportRequestType;

public record TicketFields(String name, SupportRequestType type, String email, String subject,
                           String description, String url, List<String> uploads) {

  public void validate() {
    if (name() == null || email() == null) {
      throw new IllegalArgumentException("Name and email of user requesting support is required");
    }
    if (subject() == null) {
      throw new IllegalArgumentException("Subject is required");
    }
    if (description() == null) {
      throw new IllegalArgumentException("Description is required");
    }
    if (type() == null) {
      throw new IllegalArgumentException("Type is required");
    }
    if (url() == null) {
      throw new IllegalArgumentException("URL is required");
    }
  }

}
