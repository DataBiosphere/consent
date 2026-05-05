package org.broadinstitute.consent.http.models.mail;

import java.util.Date;

public record MailMessage(
    String entityReferenceId,
    Integer emailId,
    Integer voteId,
    Integer userId,
    Integer emailType,
    Date dateSent,
    String emailText,
    String sendgridResponse,
    Integer sendgridStatus,
    Date createDate) {}
