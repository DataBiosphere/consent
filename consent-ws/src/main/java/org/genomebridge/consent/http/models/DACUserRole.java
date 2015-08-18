package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}
