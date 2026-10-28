package org.broadinstitute.consent.http.service.showcase;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;

/**
 * The showcase sections that the public lookup serves, each with its own eligibility rule on top of
 * the S2.1 catalog eligibility. Keys match the duos-ui section registry.
 */
public enum ShowcaseSection {
  /** Open access only (contracts record, original Story 8); the only editorial dataset section. */
  OPEN_ACCESS_DATASETS(
      "open-access-datasets", summary -> "open".equals(summary.accessManagement()));

  private final String slug;
  private final Predicate<ShowcaseDatasetSummary> eligibility;

  ShowcaseSection(String slug, Predicate<ShowcaseDatasetSummary> eligibility) {
    this.slug = slug;
    this.eligibility = eligibility;
  }

  public String getKey() {
    return slug;
  }

  /** True when a catalog-eligible summary can also appear in this section. */
  public boolean admits(ShowcaseDatasetSummary summary) {
    return eligibility.test(summary);
  }

  /** The section with this exact key. */
  public static Optional<ShowcaseSection> fromKey(String requested) {
    return Arrays.stream(values()).filter(section -> section.slug.equals(requested)).findFirst();
  }
}
