package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

/** This represents part of the Sam response to GET /api/config/v1/resourceTypes */
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

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
