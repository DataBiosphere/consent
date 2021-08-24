package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

public class UserStatus {

    UserInfo userInfo;
    Enabled enabled;

    public UserStatus setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
        return this;
    }

    public UserStatus setEnabled(Enabled enabled) {
        this.enabled = enabled;
        return this;
    }

    public static class UserInfo {
        String userSubjectId;
        String userEmail;

        public UserInfo setUserSubjectId(String userSubjectId) {
            this.userSubjectId = userSubjectId;
            return this;
        }

        public UserInfo setUserEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }
    }
    public static class Enabled {
        Boolean ldap;
        Boolean allUsersGroup;
        Boolean google;

        public Enabled setLdap(Boolean ldap) {
            this.ldap = ldap;
            return this;
        }

        public Enabled setAllUsersGroup(Boolean allUsersGroup) {
            this.allUsersGroup = allUsersGroup;
            return this;
        }

        public Enabled setGoogle(Boolean google) {
            this.google = google;
            return this;
        }
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}

