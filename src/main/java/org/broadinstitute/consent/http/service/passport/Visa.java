package org.broadinstitute.consent.http.service.passport;

public record Visa(String iss, String sub, Long iat, Long exp, VisaClaim visaClaim) {}
