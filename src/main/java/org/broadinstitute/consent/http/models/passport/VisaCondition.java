package org.broadinstitute.consent.http.models.passport;

public record VisaCondition(VisaClaimType type, String value, String source, VisaBy by) {
}
