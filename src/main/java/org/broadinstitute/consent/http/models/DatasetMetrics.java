package org.broadinstitute.consent.http.models;

import java.util.List;
import org.broadinstitute.consent.http.service.MetricsService.DarMetricsSummary;

public class DatasetMetrics {

  private List<DarMetricsSummary> dars;

  public List<DarMetricsSummary> getDars() {
    return dars;
  }

  public void setDars(List<DarMetricsSummary> dars) {
    this.dars = dars;
  }
}
