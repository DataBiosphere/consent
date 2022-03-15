package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

public class TosResponse {

  Enabled enabled;

  UserStatus.UserInfo userInfo;

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }

  public Enabled getEnabled() {
    return enabled;
  }

  public TosResponse setEnabled(Enabled enabled) {
    this.enabled = enabled;
    return this;
  }

  public UserStatus.UserInfo getUserInfo() {
    return userInfo;
  }

  public TosResponse setUserInfo(UserStatus.UserInfo userInfo) {
    this.userInfo = userInfo;
    return this;
  }

  public static class Enabled {
    Boolean adminEnabled;
    Boolean allUsersGroup;
    Boolean google;
    Boolean ldap;
    Boolean tosAccepted;

    public Boolean getAdminEnabled() {
      return adminEnabled;
    }

    public Enabled setAdminEnabled(Boolean adminEnabled) {
      this.adminEnabled = adminEnabled;
      return this;
    }

    public Boolean getAllUsersGroup() {
      return allUsersGroup;
    }

    public Enabled setAllUsersGroup(Boolean allUsersGroup) {
      this.allUsersGroup = allUsersGroup;
      return this;
    }

    public Boolean getGoogle() {
      return google;
    }

    public Enabled setGoogle(Boolean google) {
      this.google = google;
      return this;
    }

    public Boolean getLdap() {
      return ldap;
    }

    public Enabled setLdap(Boolean ldap) {
      this.ldap = ldap;
      return this;
    }

    public Boolean getTosAccepted() {
      return tosAccepted;
    }

    public Enabled setTosAccepted(Boolean tosAccepted) {
      this.tosAccepted = tosAccepted;
      return this;
    }
  }
}
