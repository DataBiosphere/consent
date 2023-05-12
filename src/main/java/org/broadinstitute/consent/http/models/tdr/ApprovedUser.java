package org.broadinstitute.consent.http.models.tdr;

public class ApprovedUser {

  private String email;

  public ApprovedUser(String email) {
    this.email = email;
  }

  public String getEmail() {
    return this.email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
