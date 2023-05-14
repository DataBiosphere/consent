package org.broadinstitute.consent.http.models.tdr;

import java.util.List;

public class ApprovedUsers {

  private List<ApprovedUser> approvedUsers;

  public ApprovedUsers(List<ApprovedUser> approvedUsers) {
    this.approvedUsers = approvedUsers;
  }

  public List<ApprovedUser> getApprovedUsers() {
    return this.approvedUsers;
  }

  public void setApprovedUsers(List<ApprovedUser> approvedUsers) {
    this.approvedUsers = approvedUsers;
  }
}
