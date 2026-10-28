package org.broadinstitute.consent.http.models;

import java.util.List;

/**
 * The body of a successful Data Showcase lookup.
 *
 * @param datasets summaries, with {@link ShowcaseUnavailableResult} entries in the identifiers mode
 */
public record ShowcaseLookupResponse(List<?> datasets) {}
