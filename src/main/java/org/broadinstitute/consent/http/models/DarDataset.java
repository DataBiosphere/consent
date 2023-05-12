package org.broadinstitute.consent.http.models;

public class DarDataset {

  private String referenceId;
  private Integer datasetId;

  public DarDataset() {
  }

  public DarDataset(String referenceId, Integer datasetId) {
    this.referenceId = referenceId;
    this.datasetId = datasetId;
  }

  public Integer getDatasetId() {
    return this.datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public String getReferenceId() {
    return this.referenceId;
  }

  public void setReferenceId(String referenceId) {
    this.referenceId = referenceId;
  }
}
