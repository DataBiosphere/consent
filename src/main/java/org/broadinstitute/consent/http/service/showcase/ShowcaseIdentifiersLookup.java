package org.broadinstitute.consent.http.service.showcase;

import java.util.List;

/**
 * A validated {@code identifiers} lookup.
 *
 * @param section the section whose eligibility rules apply
 * @param identifiers canonical identifiers without duplicates, in the order first submitted
 */
public record ShowcaseIdentifiersLookup(ShowcaseSection section, List<String> identifiers)
    implements ShowcaseLookup {}
