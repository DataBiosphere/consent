package org.broadinstitute.consent.http.models;

import java.util.List;

public class DatasetMetrics {

  private List<DarMetricsSummary> dars;

  public List<DarMetricsSummary> getDars() {
    return dars;
  }

  public void setDars(List<DarMetricsSummary> dars) {
    this.dars = dars;
  }
}
