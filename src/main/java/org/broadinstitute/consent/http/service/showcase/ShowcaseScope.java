package org.broadinstitute.consent.http.service.showcase;

import java.util.List;

/**
 * A ranking's {@code scope}. Values for one key are alternatives; keys must all match.
 *
 * @param dacNames exact DAC names without duplicates, or null when the scope has no {@code dac}
 *     key. An empty list matches no dataset.
 */
public record ShowcaseScope(List<String> dacNames) {}
