package org.broadinstitute.consent.http.service.showcase;

import java.util.Arrays;
import java.util.Optional;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;

/**
 * The showcase dataset sections that the public lookup serves, each with its own eligibility rule
 * on top of the S2.1 catalog eligibility. Slugs match the duos-ui section registry ({@code
 * SECTION_RANKINGS}).
 */
public enum ShowcaseSection {
  LATEST_DATA_RELEASES("latest-data-releases", false, false),
  /**
   * Open access only (original Story 8: "access type = open"). The only section that can list its
   * datasets by hand.
   */
  OPEN_ACCESS_DATASETS("open-access-datasets", true, true);

  private final String slug;
  private final boolean openAccessOnly;
  private final boolean editorial;

  ShowcaseSection(String slug, boolean openAccessOnly, boolean editorial) {
    this.slug = slug;
    this.openAccessOnly = openAccessOnly;
    this.editorial = editorial;
  }

  public String getSlug() {
    return slug;
  }

  /** True when the section shows open-access datasets only. */
  public boolean isOpenAccessOnly() {
    return openAccessOnly;
  }

  /** True when the section can list its datasets by hand, through the identifiers mode. */
  public boolean isEditorial() {
    return editorial;
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
