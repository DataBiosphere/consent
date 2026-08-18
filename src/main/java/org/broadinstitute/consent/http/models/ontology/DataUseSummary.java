package org.broadinstitute.consent.http.models.ontology;

import com.google.gson.annotations.Expose;
import java.util.List;

public class DataUseSummary {

  @Expose private List<DataUseTerm> primary;
  @Expose private List<DataUseTerm> secondary;

  public DataUseSummary() {}

  public DataUseSummary(List<DataUseTerm> primary, List<DataUseTerm> secondary) {
    this.primary = primary;
    this.secondary = secondary;
  }

  public List<DataUseTerm> getPrimary() {
    return primary;
  }

  public void setPrimary(List<DataUseTerm> primary) {
    this.primary = primary;
  }

  public List<DataUseTerm> getSecondary() {
    return secondary;
  }

  public void setSecondary(List<DataUseTerm> secondary) {
    this.secondary = secondary;
  }
}
