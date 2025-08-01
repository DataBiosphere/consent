package org.broadinstitute.consent.http.models.passport;

public record Visa(String iss, String sub, Long iat, Long exp, VisaClaim ga4gh_visa_v1) {
}
