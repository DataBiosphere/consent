package org.broadinstitute.consent.http.models.elastic_search;

public class UserTerm {

  private Integer userId;
  private String displayName;
  private InstitutionTerm institution;

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public InstitutionTerm getInstitution() {
    return institution;
  }

  public void setInstitution(
      InstitutionTerm institution) {
    this.institution = institution;
  }
}
