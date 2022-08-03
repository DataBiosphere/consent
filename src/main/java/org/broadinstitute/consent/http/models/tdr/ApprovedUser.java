package org.broadinstitute.consent.http.models.tdr;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApprovedUser {
    @JsonProperty
    private String email;

    public ApprovedUser(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
 }
