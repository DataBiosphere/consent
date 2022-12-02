package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.gson.Gson;

public class UserRole {

    @JsonProperty
    private Integer userRoleId;

    @JsonProperty
    private Integer userId;

    @JsonProperty
    private Integer roleId;

    @JsonProperty
    private String name;

    @JsonProperty
    private Integer dacId;

    public UserRole() {
    }

    public UserRole(Integer roleId, String name) {
        this.roleId = roleId;
        this.name = name;
    }

    public UserRole(Integer userRoleId, Integer userId, Integer roleId, String name, Integer dacId) {
        this.userRoleId = userRoleId;
        this.userId = userId;
        this.roleId = roleId;
        this.name = name;
        this.dacId = dacId;
    }

    public Integer getUserRoleId() {
        return userRoleId;
    }

    public void setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public Integer getDacId() {
        return dacId;
    }

    public void setDacId(Integer dacId) {
        this.dacId = dacId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.userId, this.name, this.dacId);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserRole)) {
            return false;
        }
        UserRole otherUserRole = (UserRole) o;
        return Objects.equal(this.getName(), otherUserRole.getName())
                && Objects.equal(this.getDacId(), otherUserRole.getDacId())
                && Objects.equal(this.getUserId(), otherUserRole.getUserId());
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
