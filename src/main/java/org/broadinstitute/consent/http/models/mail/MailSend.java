package org.broadinstitute.consent.http.models.mail;

import java.util.Date;
import java.util.List;

/**
 * One email sent to one or more recipients, each of whom has their own {@code email_entity} row.
 */
public record MailSend(
    Integer sendId,
    Integer emailType,
    String entityReferenceId,
    Date createDate,
    Date lastCreateDate,
    Integer recipientCount,
    List<MailSendRecipient> recipients,
    String darCode,
    List<String> datasetIdentifiers) {}
