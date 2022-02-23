package org.broadinstitute.consent.http.models.sam;

public class TosResponse {

  Enabled enabled;

  UserStatus.UserInfo userInfo;

  public Enabled getEnabled() {
    return enabled;
  }

  public void setEnabled(Enabled enabled) {
    this.enabled = enabled;
  }

  public UserStatus.UserInfo getUserInfo() {
    return userInfo;
  }

  public void setUserInfo(UserStatus.UserInfo userInfo) {
    this.userInfo = userInfo;
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

    public void setAdminEnabled(Boolean adminEnabled) {
      this.adminEnabled = adminEnabled;
    }

    public Boolean getAllUsersGroup() {
      return allUsersGroup;
    }

    public void setAllUsersGroup(Boolean allUsersGroup) {
      this.allUsersGroup = allUsersGroup;
    }

    public Boolean getGoogle() {
      return google;
    }

    public void setGoogle(Boolean google) {
      this.google = google;
    }

    public Boolean getLdap() {
      return ldap;
    }

    public void setLdap(Boolean ldap) {
      this.ldap = ldap;
    }

    public Boolean getTosAccepted() {
      return tosAccepted;
    }

    public void setTosAccepted(Boolean tosAccepted) {
      this.tosAccepted = tosAccepted;
    }
  }
}
