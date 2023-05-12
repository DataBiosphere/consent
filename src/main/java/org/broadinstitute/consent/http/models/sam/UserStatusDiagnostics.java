package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

/**
 * This represents the Sam response to GET /register/user/v2/self/diagnostics
 */
public class UserStatusDiagnostics {

  private Boolean adminEnabled;
  private Boolean enabled;
  private Boolean inAllUsersGroup;
  private Boolean inGoogleProxyGroup;
  private Boolean tosAccepted;

  public Boolean getAdminEnabled() {
    return adminEnabled;
  }

  public UserStatusDiagnostics setAdminEnabled(Boolean adminEnabled) {
    this.adminEnabled = adminEnabled;
    return this;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public UserStatusDiagnostics setEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public Boolean getInAllUsersGroup() {
    return inAllUsersGroup;
  }

  public UserStatusDiagnostics setInAllUsersGroup(Boolean inAllUsersGroup) {
    this.inAllUsersGroup = inAllUsersGroup;
    return this;
  }

  public Boolean getInGoogleProxyGroup() {
    return inGoogleProxyGroup;
  }

  public UserStatusDiagnostics setInGoogleProxyGroup(Boolean inGoogleProxyGroup) {
    this.inGoogleProxyGroup = inGoogleProxyGroup;
    return this;
  }

  public Boolean getTosAccepted() {
    return tosAccepted;
  }

  public UserStatusDiagnostics setTosAccepted(Boolean tosAccepted) {
    this.tosAccepted = tosAccepted;
    return this;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }
}
