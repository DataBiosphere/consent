package org.broadinstitute.consent.http.models;

public class ApprovedDataset {

  private int alias;
  private String darCode;
  private String datasetName;
  private String dacName;
  private String approvalDate;
  private String datasetIdentifier;

  public ApprovedDataset(int alias, String darId, String datasetName, String dacName,
      String approvalDate) {
    this.alias = alias;
    this.darCode = darId;
    this.datasetName = datasetName;
    this.dacName = dacName;
    this.approvalDate = approvalDate;
  }

  public int getAlias() {
    return alias;
  }

  public void setAlias(int alias) {
    this.alias = alias;
  }

  public String getDarCode() {
    return darCode;
  }

  public void setDarCode(String darId) {
    this.darCode = darId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public String getDacName() {
    return dacName;
  }

  public void setDacName(String dacName) {
    this.dacName = dacName;
  }

  public String getApprovalDate() {
    return approvalDate;
  }

  public void setApprovalDate(String approvalDate) {
    this.approvalDate = approvalDate;
  }

  public String getDatasetIdentifier() {
    return datasetIdentifier;
  }

  public void setDatasetIdentifier(String datasetIdentifier) {
    this.datasetIdentifier = datasetIdentifier;
  }
}
