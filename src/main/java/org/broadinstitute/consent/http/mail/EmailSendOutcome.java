package org.broadinstitute.consent.http.mail;

import com.sendgrid.Response;

public enum EmailSendOutcome {
  SENT,
  /** Not sent because email notifications are off or the recipient opted out. */
  SKIPPED,
  FAILED;

  /**
   * Mirrors SendGridAPI, which returns null when it skips a send and treats status above 202 as an
   * error.
   */
  public static EmailSendOutcome of(Response response) {
    if (response == null) {
      return SKIPPED;
    }
    return response.getStatusCode() <= 202 ? SENT : FAILED;
  }
}
