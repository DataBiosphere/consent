package org.broadinstitute.consent.http.models.passport;

public record VisaClaim(
    String type,
    Integer asserted,
    String value,
    String source,
    String by) {
}
