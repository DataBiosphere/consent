package org.broadinstitute.consent.http.models.passport;

public record Visa(String iss, String sub, Integer iat, Integer exp, VisaClaim ga4gh_visa_v1) {
}
