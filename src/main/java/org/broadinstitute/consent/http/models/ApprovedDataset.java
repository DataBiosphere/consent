package org.broadinstitute.consent.http.models;

import java.util.Date;

public class ApprovedDataset {

  private int alias;
  private String darCode;
  private String datasetName;
  private String dacName;
  private Date approvalDate;
  private String datasetIdentifier;

  public ApprovedDataset(int alias, String darId, String datasetName, String dacName,
      Date approvalDate) {
    this.alias = alias;
    this.darCode = darId;
    this.datasetName = datasetName;
    this.dacName = dacName;
    this.approvalDate = approvalDate;
    this.datasetIdentifier = Dataset.parseAliasToIdentifier(alias);
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

  public Date getApprovalDate() {
    return approvalDate;
  }

  public void setApprovalDate(Date approvalDate) {
    this.approvalDate = approvalDate;
  }

  public String getDatasetIdentifier() {
    return datasetIdentifier;
  }

  public void setDatasetIdentifier(String datasetIdentifier) {
    this.datasetIdentifier = datasetIdentifier;
  }

  public Boolean isApprovedDatasetEqual(ApprovedDataset that) {
    return this.getAlias() == that.getAlias()
        && this.getDatasetName().equals(that.getDatasetName())
        && this.getDatasetIdentifier().equals(that.getDatasetIdentifier())
        && this.getDarCode().equals(that.getDarCode())
        && this.getDacName().equals(that.getDacName())
        && (this.getApprovalDate().compareTo(that.getApprovalDate()) == 0);
  }

}
