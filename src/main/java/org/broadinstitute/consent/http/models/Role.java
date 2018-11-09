package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Role {

    @JsonProperty
    private Integer roleId;

    @JsonProperty
    private String name;

    public Role(Integer roleId, String name) {
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
}
