package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Date;
import java.util.List;

public class DACUser {

    @JsonProperty
    private Integer dacUserId;

    @JsonProperty
    private String email;

    @JsonProperty
    private String displayName;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private List<DACUserRole> roles;

    @JsonProperty
    private String accessToken;


    public DACUser() {
    }

    public DACUser(Integer dacUserId, String email, String displayName,
                   Date createDate) {
        this.dacUserId = dacUserId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
    }

    public DACUser(Integer dacUserId, String email, String displayName,
                   Date createDate, List<DACUserRole> roles) {
        this.dacUserId = dacUserId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
        this.roles = roles;
    }

    public Integer getDacUserId() {
        return dacUserId;
    }

    public void setDacUserId(Integer dacUserId) {
        this.dacUserId = dacUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<DACUserRole> getRoles() {
        return roles;
    }

    public void setRoles(List<DACUserRole> roles) {
        this.roles = roles;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public int hashCode(){
        return  dacUserId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        DACUser other = (DACUser) obj;
        return new EqualsBuilder().append(dacUserId, other.dacUserId).isEquals();
    }
}
