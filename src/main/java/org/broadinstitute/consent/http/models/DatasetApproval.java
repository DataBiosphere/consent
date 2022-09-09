package org.broadinstitute.consent.http.models;

public class DatasetApproval {
  private Boolean approval;

  public DatasetApproval() {
    this.approval = null;
  }

  public DatasetApproval(Boolean approval) {
    this.approval = approval;
  }

  public void setApproval(Boolean approval) {
    this.approval = approval;
  }

  public Boolean getApproval() {
    return approval;
  }
}
