package org.genomebridge.consent.http.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Vote {

    @JsonProperty
    private Integer voteId;

    @JsonProperty
    private Boolean vote;

    @JsonProperty
    private Integer dacUserId;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private Date updateDate;

    @JsonProperty
    private Integer electionId;

    @JsonProperty
    private String rationale;

    @JsonProperty
    private Boolean isChairPersonVote;

    public Vote() {
    }

    public Vote(Integer voteId, Boolean vote, Integer dacUserId, Date createDate, Date updateDate,
                Integer electionId, String rationale, Boolean isChairPersonVote) {
        this.voteId = voteId;
        this.vote = vote;
        this.dacUserId = dacUserId;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.electionId = electionId;
        this.rationale = rationale;
        this.isChairPersonVote = isChairPersonVote;
    }

    public Integer getVoteId() {
        return voteId;
    }

    public void setVoteId(Integer voteId) {
        this.voteId = voteId;
    }

    public Boolean getVote() {
        return vote;
    }

    public void setVote(Boolean vote) {
        this.vote = vote;
    }

    public Integer getDacUserId() {
        return dacUserId;
    }

    public void setDacUserId(Integer dacUserId) {
        this.dacUserId = dacUserId;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Integer getElectionId() {
        return electionId;
    }

    public void setElectionId(Integer electionId) {
        this.electionId = electionId;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public Boolean getIsChairPersonVote() {
        return isChairPersonVote;
    }

    public void setIsChairPersonVote(Boolean isChairPersonVote) {
        this.isChairPersonVote = isChairPersonVote;
    }
}
