package org.broadinstitute.consent.http.models;

import java.util.List;

public record DatasetUpdate (
    String name,
    Integer dacId,
    List<DatasetProperty> properties
){

  public String getName() {
    return this.name;
  }

  public Integer getDacId() {
    return this.dacId;
  }

  public List<DatasetProperty> getDatasetProperties() {
    return this.properties;
  }
}
