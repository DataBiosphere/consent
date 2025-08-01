package org.broadinstitute.consent.http.models.passport;

public record VisaClaim(
    String type,
    Long asserted,
    String value,
    String source,
    String by) {
}
