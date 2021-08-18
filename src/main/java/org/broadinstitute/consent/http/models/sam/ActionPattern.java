package org.broadinstitute.consent.http.models.sam;

public class ActionPattern {

  private Boolean authDomainConstrainable;
  private String description;
  private String value;

  public Boolean getAuthDomainConstrainable() {
    return authDomainConstrainable;
  }

  public void setAuthDomainConstrainable(Boolean authDomainConstrainable) {
    this.authDomainConstrainable = authDomainConstrainable;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
