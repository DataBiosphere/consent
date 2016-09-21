package org.broadinstitute.consent.http.models.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class ElectionStatusDTO {

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private String electionStatus;

    public ElectionStatusDTO() {
    }

    public ElectionStatusDTO(Date createDate, String electionStatus) {
        this.createDate = createDate;
        this.electionStatus = electionStatus;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getElectionStatus() {
        return electionStatus;
    }

    public void setElectionStatus(String electionStatus) {
        this.electionStatus = electionStatus;
    }
}
