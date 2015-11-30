package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class DACUserRole {

    @JsonProperty
    private Integer roleId;

    @JsonProperty
    private String name;

    public DACUserRole(){
    }

    public DACUserRole(Integer roleId, String name){
        this.roleId = roleId;
        this.name = name;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int hashCode() {
        return Objects.hashCode(this.roleId, this.name);
    }

    public boolean equals(Object o) {
        if(!(o instanceof DACUserRole)) { return false; }
        DACUserRole otherConsent = (DACUserRole) o;
        return Objects.equal(this.getName(), otherConsent.getName());
    }
}
