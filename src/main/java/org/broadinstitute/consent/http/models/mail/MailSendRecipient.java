package org.broadinstitute.consent.http.models.mail;

/** One recipient of a {@link MailSend}. */
public record MailSendRecipient(Integer userId, String displayName, Boolean delivered) {}
