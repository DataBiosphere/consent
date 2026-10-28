package org.broadinstitute.consent.http.service.showcase;

/**
 * A validated {@code stats} lookup.
 *
 * @param scope the scope, or null for the whole public catalog
 */
public record ShowcaseStatsLookup(ShowcaseScope scope) implements ShowcaseLookup {}
