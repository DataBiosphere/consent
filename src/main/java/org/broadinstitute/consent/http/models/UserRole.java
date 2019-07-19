package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;

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

    /**
     * Convenience method for backwards compatibility support for older clients.
     *
     * @param json A json string that may or may not be correctly structured as a UserRole
     */
    public UserRole(String json) {
        Gson gson = new Gson();
        UserRole ur = gson.fromJson(json, UserRole.class);
        if (!StringUtils.isEmpty(ur.getName())) {
            this.name = ur.getName();
        }
        if (ur.getRoleId() != null) {
            this.roleId = ur.getRoleId();
        } else {
            UserRoles r = UserRoles.getUserRoleFromName(this.getName());
            if (r != null) {
                this.setRoleId(r.getRoleId());
            }
        }
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
        return Objects.hashCode(this.roleId, this.name);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserRole)) {
            return false;
        }
        UserRole otherConsent = (UserRole) o;
        return Objects.equal(this.getRoleId(), otherConsent.getRoleId());
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
