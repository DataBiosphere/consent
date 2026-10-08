package org.broadinstitute.consent.http.models.mail;

import java.util.Date;

/** A {@link MailMessage} without its email body or SendGrid response, for listing many at once. */
public record MailMessageSummary(
    String entityReferenceId,
    Integer emailId,
    Integer voteId,
    Integer userId,
    Integer emailType,
    Date dateSent,
    Integer sendgridStatus,
    Date createDate) {}
