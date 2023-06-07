package org.broadinstitute.consent.http.models;

import java.util.List;

public record DatasetUpdate (
    String name,
    Boolean needsApproval,
    Boolean active,
    Integer dacId,
    List<DatasetProperty> properties
){

  public String getName() {
    return this.name;
  }

  public Boolean getNeedsApproval() {
    return this.needsApproval;
  }
  public Boolean getActive() {
    return this.active;
  }

  public Integer getDacId() {
    return this.dacId;
  }

  public List<DatasetProperty> getDatasetProperties() {
    return this.properties;
  }
}
