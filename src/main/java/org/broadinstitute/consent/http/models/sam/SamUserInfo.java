package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

public class SamUserInfo {

  String userSubjectId;
  String userEmail;
  Boolean enabled;

  public String getUserSubjectId() {
    return userSubjectId;
  }

  public SamUserInfo setUserSubjectId(String userSubjectId) {
    this.userSubjectId = userSubjectId;
    return this;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public SamUserInfo setUserEmail(String userEmail) {
    this.userEmail = userEmail;
    return this;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public SamUserInfo setEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
