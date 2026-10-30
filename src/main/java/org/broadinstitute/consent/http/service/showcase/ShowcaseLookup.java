package org.broadinstitute.consent.http.service.showcase;

/** A validated public Data Showcase lookup, one type for each mode. */
public sealed interface ShowcaseLookup
    permits ShowcaseIdentifiersLookup,
        ShowcaseRankingLookup,
        ShowcaseAllLookup,
        ShowcaseStatsLookup {}
