package org.broadinstitute.consent.http.service.showcase;

import java.util.List;

/**
 * A validated {@code ranking} lookup.
 *
 * @param section the section whose eligibility rules apply
 * @param ranking the ranking that orders the datasets after the pins
 * @param scope the scope, or null for the whole public catalog
 * @param limit the most datasets to return, pins included
 * @param pins canonical identifiers to place first when eligible, without duplicates
 * @param exclusions canonical identifiers never to return, without duplicates
 */
public record ShowcaseRankingLookup(
    ShowcaseSection section,
    ShowcaseRanking ranking,
    ShowcaseScope scope,
    int limit,
    List<String> pins,
    List<String> exclusions)
    implements ShowcaseLookup {}
