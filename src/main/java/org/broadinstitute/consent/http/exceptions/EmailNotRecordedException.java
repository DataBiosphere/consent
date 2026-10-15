package org.broadinstitute.consent.http.exceptions;

import org.broadinstitute.consent.http.mail.EmailSendOutcome;

/** The email went to SendGrid with this outcome, but its email_entity row could not be written. */
public class EmailNotRecordedException extends RuntimeException {

  private final EmailSendOutcome outcome;

  public EmailNotRecordedException(EmailSendOutcome outcome, Throwable cause) {
    super("Email " + outcome + " but not recorded", cause);
    this.outcome = outcome;
  }

  public EmailSendOutcome getOutcome() {
    return outcome;
  }
}
