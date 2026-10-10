package org.broadinstitute.consent.http.models.mail;

import java.util.Date;
import java.util.UUID;

public record MailMessageInsert(
    String entityReferenceId,
    Integer voteId,
    Integer userId,
    Integer emailType,
    Date dateSent,
    String emailText,
    String sendgridResponse,
    Integer sendgridStatus,
    UUID sendId) {

  public MailMessageInsert(
      String entityReferenceId,
      Integer voteId,
      Integer userId,
      Integer emailType,
      Date dateSent,
      String emailText,
      String sendgridResponse,
      Integer sendgridStatus) {
    this(
        entityReferenceId,
        voteId,
        userId,
        emailType,
        dateSent,
        emailText,
        sendgridResponse,
        sendgridStatus,
        null);
  }
}
