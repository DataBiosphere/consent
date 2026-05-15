package org.broadinstitute.consent.http.models.mail;

import java.util.Date;

public record MailMessageInsert(
    String entityReferenceId,
    Integer voteId,
    Integer userId,
    Integer emailType,
    Date dateSent,
    String emailText,
    String sendgridResponse,
    Integer sendgridStatus) {}
