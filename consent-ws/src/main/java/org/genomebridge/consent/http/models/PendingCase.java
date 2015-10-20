package org.genomebridge.consent.http.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class PendingCase implements Comparable<PendingCase>{

    @JsonProperty
    private String referenceId;

    @JsonProperty
    private String logged;

    @JsonProperty
    private Boolean alreadyVoted;

    @JsonProperty
    private Boolean isReminderSent;

    @JsonProperty
    private Boolean isFinalVote;

    @JsonProperty
    private String status;

    @JsonProperty
    private String electionStatus;

    @JsonProperty
    private Integer electionId;

    @JsonProperty
    private Integer voteId;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private Integer totalVotes;

    @JsonProperty
    private Integer votesLogged;

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Boolean getIsReminderSent() {
        return isReminderSent;
    }

    public void setIsReminderSent(Boolean isReminderSent) {
        this.isReminderSent = isReminderSent;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Integer getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Integer totalVotes) {
        this.totalVotes = totalVotes;
    }

    public Integer getVotesLogged() {
        return votesLogged;
    }

    public void setVotesLogged(Integer votesLogged) {
        this.votesLogged = votesLogged;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getLogged() {
        return logged;
    }

    public void setLogged(String logged) {
        this.logged = logged;
    }

    public Boolean getAlreadyVoted() {
        return alreadyVoted;
    }

    public void setAlreadyVoted(Boolean alreadyVoted) {
        this.alreadyVoted = alreadyVoted;
    }

    public Boolean getIsFinalVote() {
        return isFinalVote;
    }

    public void setIsFinalVote(Boolean isFinalVote) {
        this.isFinalVote = isFinalVote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVoteId() {
        return voteId;
    }

    public void setVoteId(Integer voteId) {
        this.voteId = voteId;
    }

    public String getElectionStatus() {
        return electionStatus;
    }

    public void setElectionStatus(String electionStatus) {
        this.electionStatus = electionStatus;
    }

    public Integer getElectionId() {
        return electionId;
    }

    public void setElectionId(Integer electionId) {
        this.electionId = electionId;
    }

    @Override
    public int compareTo(PendingCase o) {
        return this.getCreateDate().compareTo(o.getCreateDate());
    }


}
