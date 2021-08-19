package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

public class SamUserInfo {

  String userSubjectId;
  String userEmail;
  Boolean enabled;

  public String getUserSubjectId() {
    return userSubjectId;
  }

  public void setUserSubjectId(String userSubjectId) {
    this.userSubjectId = userSubjectId;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
