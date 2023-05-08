package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

/**
 * This represents the Sam response when a new user is created via POST /register/user/v2/self
 */
public class UserStatus {

    private UserInfo userInfo;
    private Enabled enabled;

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public UserStatus setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
        return this;
    }

    public Enabled getEnabled() {
        return enabled;
    }

    public UserStatus setEnabled(Enabled enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static class UserInfo {
        private String userSubjectId;
        private String userEmail;

        public String getUserSubjectId() {
            return userSubjectId;
        }

        public UserInfo setUserSubjectId(String userSubjectId) {
            this.userSubjectId = userSubjectId;
            return this;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public UserInfo setUserEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }
    }

    public static class Enabled {
        private Boolean ldap;
        private Boolean allUsersGroup;
        private Boolean google;

        public Boolean getLdap() {
            return ldap;
        }

        public Enabled setLdap(Boolean ldap) {
            this.ldap = ldap;
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
    }
}
