package org.broadinstitute.consent.http.service.showcase;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;

/**
 * The showcase dataset sections that the public lookup serves, each with its own eligibility rule
 * on top of the S2.1 catalog eligibility, and the rankings it accepts. Slugs match the duos-ui
 * section registry ({@code SECTION_RANKINGS}).
 */
public enum ShowcaseSection {
  LATEST_DATA_RELEASES("latest-data-releases", false, false, Set.of(ShowcaseRanking.NEWEST)),
  /**
   * Open access only (original Story 8: "access type = open"). The only section that can list its
   * datasets by hand.
   */
  OPEN_ACCESS_DATASETS("open-access-datasets", true, true, Set.of(ShowcaseRanking.NEWEST));

  private final String slug;
  private final boolean openAccessOnly;
  private final boolean editorial;
  private final Set<ShowcaseRanking> rankings;

  ShowcaseSection(
      String slug, boolean openAccessOnly, boolean editorial, Set<ShowcaseRanking> rankings) {
    this.slug = slug;
    this.openAccessOnly = openAccessOnly;
    this.editorial = editorial;
    this.rankings = rankings;
  }

  public String getSlug() {
    return slug;
  }

  /** True when the section shows open-access datasets only. The ranking query reads this too. */
  public boolean isOpenAccessOnly() {
    return openAccessOnly;
  }

  /** True when the section can list its datasets by hand, through the identifiers mode. */
  public boolean isEditorial() {
    return editorial;
  }

  public boolean accepts(ShowcaseRanking ranking) {
    return rankings.contains(ranking);
  }

  /** True when a catalog-eligible summary can also appear in this section. */
  public boolean admits(ShowcaseDatasetSummary summary) {
    return !openAccessOnly || "open".equals(summary.accessManagement());
  }

  /** The section with this exact slug. */
  public static Optional<ShowcaseSection> fromSlug(String requested) {
    return Arrays.stream(values()).filter(section -> section.slug.equals(requested)).findFirst();
  }
}
