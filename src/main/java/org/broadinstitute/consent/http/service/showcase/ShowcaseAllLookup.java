package org.broadinstitute.consent.http.service.showcase;

/**
 * A validated {@code all} lookup: one page of every public-eligible dataset, in alias order.
 *
 * @param pageSize the most datasets on the page
 * @param afterAlias the last alias of the previous page, or null for the first page
 */
public record ShowcaseAllLookup(int pageSize, Integer afterAlias) implements ShowcaseLookup {}
