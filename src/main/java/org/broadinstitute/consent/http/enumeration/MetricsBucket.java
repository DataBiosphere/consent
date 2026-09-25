package org.broadinstitute.consent.http.enumeration;

import java.util.Locale;

/** A reporting time bucket, named as Postgres {@code date_trunc} expects. */
public enum MetricsBucket {
  DAY,
  WEEK,
  MONTH,
  QUARTER;

  public String truncUnit() {
    return name().toLowerCase(Locale.ROOT);
  }
}
