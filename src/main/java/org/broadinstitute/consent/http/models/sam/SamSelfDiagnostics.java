package org.broadinstitute.consent.http.models.sam;

import com.google.gson.Gson;

public class SamSelfDiagnostics {

    Boolean enabled;
    Boolean inAllUsersGroup;
    Boolean inGoogleProxyGroup;

    public Boolean getEnabled() {
        return enabled;
    }

    public SamSelfDiagnostics setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getInAllUsersGroup() {
        return inAllUsersGroup;
    }

    public SamSelfDiagnostics setInAllUsersGroup(Boolean inAllUsersGroup) {
        this.inAllUsersGroup = inAllUsersGroup;
        return this;
    }

    public Boolean getInGoogleProxyGroup() {
        return inGoogleProxyGroup;
    }

    public SamSelfDiagnostics setInGoogleProxyGroup(Boolean inGoogleProxyGroup) {
        this.inGoogleProxyGroup = inGoogleProxyGroup;
        return this;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
