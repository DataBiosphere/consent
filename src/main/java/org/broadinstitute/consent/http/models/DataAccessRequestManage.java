package org.broadinstitute.consent.http.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the basic elements that are necessary for a list of Data Access Requests
 * that can be managed by an admin, chair, or member.
 */
@JsonInclude(Include.NON_NULL)
public class DataAccessRequestManage {

    private DataAccessRequest dar;
    private Election election;
    private List<Vote> votes;
    private Dac dac;

    @Deprecated
    private String referenceId;
    @Deprecated
    private String logged;
    @Deprecated
    private Boolean alreadyVoted;
    @Deprecated
    private Boolean isReminderSent;
    @Deprecated
    private Boolean isFinalVote;
    @Deprecated
    private Integer voteId;
    @Deprecated
    private Integer totalVotes;
    @Deprecated
    private Integer votesLogged;
    @Deprecated
    private Integer rpElectionId;
    @Deprecated
    private Integer rpVoteId;
    private String consentGroupName;
    @Deprecated
    private String electionStatus;
    @Deprecated
    private String status;
    @Deprecated
    private String rus;
    @Deprecated
    private String dataRequestId;
    @Deprecated
    private String projectTitle;
    @Deprecated
    private String frontEndId;
    @Deprecated
    private Integer electionId;
    @Deprecated
    private Long createDate;
    @Deprecated
    private Long sortDate;
    @Deprecated
    private Boolean electionVote;
    @Deprecated
    private Boolean isCanceled;
    @Deprecated
    private Boolean needsApproval;
    @Deprecated
    private String dataSetElectionResult;
    @Deprecated
    private Integer datasetId;
    @Deprecated
    private Integer dacId;
    @Deprecated
    private List<String> errors = new ArrayList<>();
    @Deprecated
    private User ownerUser;

    public DataAccessRequestManage() {
    }

    public DataAccessRequest getDar() {
        return dar;
    }

    public void setDar(DataAccessRequest dar) {
        this.dar = dar;
    }

    public Election getElection() {
        return election;
    }

    public void setElection(Election election) {
        this.election = election;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public void setVotes(List<Vote> votes) {
        this.votes = votes;
    }

    public Dac getDac() {
        return dac;
    }

    public void setDac(Dac dac) {
        this.dac = dac;
    }

    public String getReferenceId() {
        return referenceId;
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

    public Boolean getReminderSent() {
        return isReminderSent;
    }

    public void setReminderSent(Boolean reminderSent) {
        isReminderSent = reminderSent;
    }

    public Boolean getFinalVote() {
        return isFinalVote;
    }

    public void setFinalVote(Boolean finalVote) {
        isFinalVote = finalVote;
    }

    public Integer getVoteId() {
        return voteId;
    }

    public void setVoteId(Integer voteId) {
        this.voteId = voteId;
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

    public Integer getRpElectionId() {
        return rpElectionId;
    }

    public void setRpElectionId(Integer rpElectionId) {
        this.rpElectionId = rpElectionId;
    }

    public Integer getRpVoteId() {
        return rpVoteId;
    }

    public void setRpVoteId(Integer rpVoteId) {
        this.rpVoteId = rpVoteId;
    }

    public String getConsentGroupName() {
        return consentGroupName;
    }

    public void setConsentGroupName(String consentGroupName) {
        this.consentGroupName = consentGroupName;
    }

    public String getElectionStatus() {
        return electionStatus;
    }

    public void setElectionStatus(String electionStatus) {
        this.electionStatus = electionStatus;
    }

    public String getRus() {
        return rus;
    }

    public void setRus(String rus) {
        this.rus = rus;
    }

    public String getDataRequestId() {
        return dataRequestId;
    }

    public void setDataRequestId(String dataRequestId) {
        this.dataRequestId = dataRequestId;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public void setProjectTitle(String projectTitle) {
        this.projectTitle = projectTitle;
    }

    public String getFrontEndId() {
        return frontEndId;
    }

    public void setFrontEndId(String frontEndId) {
        this.frontEndId = frontEndId;
    }

    public Integer getElectionId() {
        return electionId;
    }

    public void setElectionId(Integer electionId) {
        this.electionId = electionId;
    }

    public Long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Long createDate) {
        this.createDate = createDate;
    }

    public Long getSortDate() {
        return sortDate;
    }

    public void setSortDate(Long sortDate) {
        this.sortDate = sortDate;
    }

    public Boolean getElectionVote() {
        return electionVote;
    }

    public void setElectionVote(Boolean electionVote) {
        this.electionVote = electionVote;
    }

    public Boolean getIsCanceled() {
        return isCanceled;
    }

    public void setIsCanceled(Boolean isCanceled) {
        this.isCanceled = isCanceled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getNeedsApproval() {
        return needsApproval;
    }

    public void setNeedsApproval(Boolean needsApproval) {
        this.needsApproval = needsApproval;
    }

    public String getDataSetElectionResult() {
        return dataSetElectionResult;
    }

    public void setDataSetElectionResult(String dataSetElectionResult) {
        this.dataSetElectionResult = dataSetElectionResult;
    }

    public Integer getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public Integer getDacId() {
        return dacId;
    }

    public void setDacId(Integer dacId) {
        this.dacId = dacId;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public User getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(User ownerUser) {
        this.ownerUser = ownerUser;
    }

    public void addError(String error) {
        errors.add(error);
    }

}
