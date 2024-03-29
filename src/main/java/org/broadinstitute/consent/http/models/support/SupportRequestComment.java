package org.broadinstitute.consent.http.models.support;

/**
 * Represents comment describing issue for a Zendesk support ticket
 * https://broadinstitute.zendesk.com/api/v2/requests.json
 */
public class SupportRequestComment {

  private String body;

  public SupportRequestComment(String body) {
    this.body = body;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
