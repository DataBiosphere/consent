package org.broadinstitute.consent.http.service.passport;

public record VisaClaim(String type, Long asserted, Object value, String source, String by) {}
