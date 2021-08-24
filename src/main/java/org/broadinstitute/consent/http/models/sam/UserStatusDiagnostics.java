package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

public class UserStatusDiagnostics {

    Boolean enabled;
    Boolean inAllUsersGroup;
    Boolean inGoogleProxyGroup;

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

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
