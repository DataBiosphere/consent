package org.broadinstitute.consent.http.models.mail;

import java.util.Date;
import java.util.List;
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
    UUID sendId,
    List<Integer> datasetIds) {

  public MailMessageInsert(
      String entityReferenceId,
      Integer voteId,
      Integer userId,
      Integer emailType,
      Date dateSent,
      String emailText,
      String sendgridResponse,
      Integer sendgridStatus,
      UUID sendId) {
    this(
        entityReferenceId,
        voteId,
        userId,
        emailType,
        dateSent,
        emailText,
        sendgridResponse,
        sendgridStatus,
        sendId,
        null);
  }
}
