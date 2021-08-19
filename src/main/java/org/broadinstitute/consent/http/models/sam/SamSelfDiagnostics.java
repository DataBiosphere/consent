package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

public class SamSelfDiagnostics {

    Boolean enabled;
    Boolean inAllUsersGroup;
    Boolean inGoogleProxyGroup;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getInAllUsersGroup() {
        return inAllUsersGroup;
    }

    public void setInAllUsersGroup(Boolean inAllUsersGroup) {
        this.inAllUsersGroup = inAllUsersGroup;
    }

    public Boolean getInGoogleProxyGroup() {
        return inGoogleProxyGroup;
    }

    public void setInGoogleProxyGroup(Boolean inGoogleProxyGroup) {
        this.inGoogleProxyGroup = inGoogleProxyGroup;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
