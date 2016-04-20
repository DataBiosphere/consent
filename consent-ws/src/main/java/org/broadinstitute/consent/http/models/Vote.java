package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

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
    private String type;

    @JsonProperty
    private Boolean isReminderSent;

    @JsonProperty
    private Boolean hasConcerns;

    public Vote() {
    }

    public Vote(Integer voteId, Boolean vote, Integer dacUserId, Date createDate, Date updateDate,
                Integer electionId, String rationale, String type, Boolean isReminderSent, Boolean hasConcerns) {
        this.voteId = voteId;
        this.vote = vote;
        this.dacUserId = dacUserId;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.electionId = electionId;
        this.rationale = rationale;
        this.type = type;
        this.isReminderSent = isReminderSent;
        this.hasConcerns = hasConcerns;
    }

    public void initVote(Integer dacUserId, Date createDate, Date updateDate,
                         String rationale, Boolean isReminderSent, Boolean hasConcerns, Boolean vote) {
        this.dacUserId = dacUserId;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.rationale = rationale;
        this.isReminderSent = isReminderSent;
        this.hasConcerns = hasConcerns;
        this.vote = vote;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsReminderSent() {
        return isReminderSent;
    }

    public void setIsReminderSent(Boolean isReminderSent) {
        this.isReminderSent = isReminderSent;
    }

    public Boolean getHasConcerns() {
        return hasConcerns;
    }

    public void setHasConcerns(Boolean hasConcerns) {
        this.hasConcerns = hasConcerns;
    }
}