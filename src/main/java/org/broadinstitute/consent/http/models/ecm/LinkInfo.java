package org.broadinstitute.consent.http.models.ecm;

public record LinkInfo(String externalUserId, String expirationTimestamp, Boolean authenticated) {
}
