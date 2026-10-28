package org.broadinstitute.consent.http.models;

import java.util.List;

/**
 * One page of the Data Showcase {@code all} lookup.
 *
 * @param datasets the summaries on this page, in identifier order
 * @param nextCursor the cursor for the next page, or null on the last page
 */
public record ShowcasePageResponse(List<ShowcaseDatasetSummary> datasets, String nextCursor) {}
