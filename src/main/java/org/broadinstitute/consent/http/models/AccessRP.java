package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AccessRP {

    @JsonProperty
    private Integer id;

    @JsonProperty
    private Integer electionAccessId;

    @JsonProperty
    private Integer electionRPId;


    public AccessRP(Integer id, Integer electionAccessId , Integer electionRPId ){
        this.id = id;
        this.electionAccessId = electionAccessId;
        this.electionRPId = electionRPId;

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getElectionAccessId() {
        return electionAccessId;
    }

    public void setElectionAccessId(Integer electionAccessId) {
        this.electionAccessId = electionAccessId;
    }

    public Integer getElectionRPId() {
        return electionRPId;
    }

    public void setElectionRPId(Integer electionRPId) {
        this.electionRPId = electionRPId;
    }
}