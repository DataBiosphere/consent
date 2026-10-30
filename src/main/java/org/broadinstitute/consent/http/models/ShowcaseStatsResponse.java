package org.broadinstitute.consent.http.models;

/**
 * The body of a {@code stats} lookup: the catalog-eligible datasets in the scope, and their
 * distinct studies.
 */
public record ShowcaseStatsResponse(long studyCount, long datasetCount) {}
