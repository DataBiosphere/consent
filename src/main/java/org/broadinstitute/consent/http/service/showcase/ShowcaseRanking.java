package org.broadinstitute.consent.http.service.showcase;

import java.util.Arrays;
import java.util.Optional;

/** The fixed rankings a computed shelf can use. S6.1 adds the others. */
public enum ShowcaseRanking {
  /**
   * Newest first by the S1.1 "new in DUOS" date: {@code create_date} for open and external
   * datasets, {@code dac_approval_date} for controlled ones. Datasets on the same date are in
   * identifier order, and a dataset with no date is left out.
   */
  NEWEST("newest");

  private final String slug;

  ShowcaseRanking(String slug) {
    this.slug = slug;
  }

  public String getSlug() {
    return slug;
  }

  /** The ranking with this exact slug. */
  public static Optional<ShowcaseRanking> fromSlug(String requested) {
    return Arrays.stream(values()).filter(ranking -> ranking.slug.equals(requested)).findFirst();
  }
}
