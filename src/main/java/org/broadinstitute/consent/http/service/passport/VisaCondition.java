package org.broadinstitute.consent.http.service.passport;

public record VisaCondition(VisaClaimType type, String value, String source, VisaBy by) {}
