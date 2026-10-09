package org.broadinstitute.consent.http.models.mail;

import java.util.Date;
import java.util.List;

/**
 * One email sent to one or more recipients. Each recipient has its own {@code email_entity} row, so
 * a send is the rows of one type and entity reference created close together.
 */
public record MailSend(
    Integer sendId,
    Integer emailType,
    String entityReferenceId,
    Date createDate,
    Integer recipientCount,
    List<MailSendRecipient> recipients) {}
