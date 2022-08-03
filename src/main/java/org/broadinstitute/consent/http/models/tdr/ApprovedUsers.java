package org.broadinstitute.consent.http.models.tdr;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ApprovedUsers {
    @JsonProperty
    private List<ApprovedUser> approvedUsers;

    public ApprovedUsers(List<ApprovedUser> approvedUsers) {
        this.approvedUsers = approvedUsers;
    }

    public List<ApprovedUser> getApprovedUsers() {
        return this.approvedUsers;
    }

    public void setApprovedUsers(List<ApprovedUser> approvedUsers) {
        this.approvedUsers = approvedUsers;
    }
}
