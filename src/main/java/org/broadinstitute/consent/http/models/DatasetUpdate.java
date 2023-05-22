package org.broadinstitute.consent.http.models;

import java.util.List;

public record DatasetUpdate (
    String name,
    Integer datasetId,
    DataUse dataUse,
    Integer dacId,
    List<DatasetProperty> datasetProperties,
    String description,
    List<String> dataTypes,
    String piName,
    Boolean publicVisibility
){

  public List<DatasetProperty> getDatasetProperties() {
    return this.datasetProperties;
  }

}
