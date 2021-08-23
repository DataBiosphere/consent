package org.broadinstitute.consent.http.models.sam;

public class ActionPattern {

  private Boolean authDomainConstrainable;
  private String description;
  private String value;

  public Boolean getAuthDomainConstrainable() {
    return authDomainConstrainable;
  }

  public ActionPattern setAuthDomainConstrainable(Boolean authDomainConstrainable) {
    this.authDomainConstrainable = authDomainConstrainable;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public ActionPattern setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getValue() {
    return value;
  }

  public ActionPattern setValue(String value) {
    this.value = value;
    return this;
  }
}
