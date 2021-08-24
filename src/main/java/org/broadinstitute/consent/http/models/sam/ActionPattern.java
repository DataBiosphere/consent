package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

public class ActionPattern {

  private Boolean authDomainConstrainable;
  private String description;
  private String value;

  public ActionPattern setAuthDomainConstrainable(Boolean authDomainConstrainable) {
    this.authDomainConstrainable = authDomainConstrainable;
    return this;
  }

  public ActionPattern setDescription(String description) {
    this.description = description;
    return this;
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
