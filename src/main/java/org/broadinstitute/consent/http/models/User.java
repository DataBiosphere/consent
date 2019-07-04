package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;

public class User {

    @JsonProperty
    private Integer userId;

    @JsonProperty
    private String email;

    @JsonProperty
    private String displayName;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private String additionalEmail;

    @JsonProperty
    private List<UserRole> roles;

    public User() {
    }

    public User(Integer userId, String email, String displayName, Date createDate) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
    }

    public User(Integer userId, String email, String displayName, Date createDate, String additionalEmail) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
        this.additionalEmail = additionalEmail;
    }

    public User(Integer userId, String email, String displayName,
                String isApproved, Date createDate, List<UserRole> roles, String additionalEmail) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.createDate = createDate;
        this.roles = roles;
        this.additionalEmail = additionalEmail;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public List<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(List<UserRole> roles) {
        this.roles = roles;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getAdditionalEmail() {
        return additionalEmail;
    }

    public void setAdditionalEmail(String additionalEmail) {
        this.additionalEmail = additionalEmail;
    }

    @Override
    public int hashCode(){
        return userId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        User other = (User) obj;
        return new EqualsBuilder().append(userId, other.userId).isEquals();
    }
}
