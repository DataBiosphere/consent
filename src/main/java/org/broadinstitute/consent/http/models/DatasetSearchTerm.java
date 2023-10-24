package org.broadinstitute.consent.http.models;

import java.util.List;

public class DatasetSearchTerm {

  private Integer datasetId = 0;
  private String datasetIdentifier = "";
  // nosemgrep
  private String description = "";
  private String studyName = "";
  private Integer studyId = 0;
  private Integer participantCount = 0;
  private String phenotype = "";
  private String species = "";
  private String piName = "";
  private String dataSubmitter = "";
  private String dataCustodian = "";
  private List<String> dataUse = List.of("");
  private List<String> dataTypes = List.of("");
  private String dataLocation = "";
  private String dacName = "";
  private Boolean accessManagement = false;
  private List<Integer> approvedUsers = List.of(0);

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }
}
