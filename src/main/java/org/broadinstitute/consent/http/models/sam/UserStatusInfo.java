package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

/**
 * This represents the Sam response to GET /register/user/v2/self/info
 */
public class UserStatusInfo {

  private Boolean adminEnabled;
  private String userSubjectId;
  private String userEmail;
  private Boolean enabled;

  public Boolean getAdminEnabled() {
    return adminEnabled;
  }

  public UserStatusInfo setAdminEnabled(Boolean adminEnabled) {
    this.adminEnabled = adminEnabled;
    return this;
  }

  public String getUserSubjectId() {
    return userSubjectId;
  }

  public UserStatusInfo setUserSubjectId(String userSubjectId) {
    this.userSubjectId = userSubjectId;
    return this;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public UserStatusInfo setUserEmail(String userEmail) {
    this.userEmail = userEmail;
    return this;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public UserStatusInfo setEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
