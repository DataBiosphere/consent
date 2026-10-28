package org.broadinstitute.consent.http.models;

import java.util.List;

/**
 * The body of a successful Data Showcase lookup.
 *
 * @param datasets {@link ShowcaseDatasetSummary} and {@link ShowcaseUnavailableResult} entries
 */
public record ShowcaseLookupResponse(List<Object> datasets) {}
