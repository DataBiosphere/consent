package org.broadinstitute.consent.http.models.mail;

public record MailSendRecipient(Integer userId, String displayName, Boolean sent) {}
