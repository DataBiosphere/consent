package org.broadinstitute.consent.http.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;

public class Election {

    @JsonProperty
    private Integer electionId;

    @JsonProperty
    private String electionType;

    @JsonProperty
    private Boolean finalVote;

    @JsonProperty
    private String status;

    @JsonProperty
    private Date createDate;

    @JsonProperty
    private Date lastUpdate;

    @JsonProperty
    private Date finalVoteDate;

    @JsonProperty
    private String referenceId;

    @JsonProperty
    private String finalRationale;

    @JsonProperty
    private Boolean finalAccessVote;

    @JsonProperty
    public UseRestriction useRestriction;

    @JsonProperty
    private String translatedUseRestriction;

    @JsonProperty
    private Integer dataSetId;

    public Election() {
    }
    public Election(Integer electionId, String electionType,
                    String status, Date createDate,
                    String referenceId, Date lastUpdate , Boolean finalAccessVote, Integer dataSetId) {
        this.electionId = electionId;
        this.electionType = electionType;
        this.status = status;
        this.createDate = createDate;
        this.referenceId = referenceId;
        this.lastUpdate = lastUpdate;
        this.finalAccessVote = finalAccessVote;
        this.dataSetId = dataSetId;
    }


    public Election(Integer electionId, String electionType,
                    Boolean finalVote, String finalRationale, String status, Date createDate,
                    Date finalVoteDate, String referenceId, Date lastUpdate , Boolean finalAccessVote,
                    UseRestriction useRestriction, String translatedUseRestriction) {
        this.electionId = electionId;
        this.electionType = electionType;
        this.status = status;
        this.createDate = createDate;
        this.referenceId = referenceId;
        this.lastUpdate = lastUpdate;
        this.finalAccessVote = finalAccessVote;
        this.finalVote = finalVote;
        this.finalRationale = finalRationale;
        this.finalVoteDate = finalVoteDate;
        this.useRestriction = useRestriction;
        this.translatedUseRestriction = translatedUseRestriction;
    }

    public Election(Integer electionId, String electionType, String status, Date createDate, String referenceId, Integer dataSetId) {
        this.electionId = electionId;
        this.electionType = electionType;
        this.status = status;
        this.createDate = createDate;
        this.referenceId = referenceId;
        this.dataSetId = dataSetId;
    }

    public Integer getElectionId() {
        return electionId;
    }

    public void setElectionId(Integer electionId) {
        this.electionId = electionId;
    }

    public String getElectionType() {
        return electionType;
    }

    public void setElectionType(String electionType) {
        this.electionType = electionType;
    }

    public Boolean getFinalVote() {
        return finalVote;
    }

    public void setFinalVote(Boolean finalVote) {
        this.finalVote = finalVote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastUpdateDate() {
        return lastUpdate;
    }

    public void setLastUpdateDate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getFinalVoteDate() {
        return finalVoteDate;
    }

    public void setFinalVoteDate(Date finalVoteDate) {
        this.finalVoteDate = finalVoteDate;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getFinalRationale() {
        return finalRationale;
    }

    public void setFinalRationale(String finalRationale) {
        this.finalRationale = finalRationale;
    }

    public Boolean getFinalAccessVote() {
        return finalAccessVote;
    }

    public void setFinalAccessVote(Boolean finalAccessVote) {
        this.finalAccessVote = finalAccessVote;
    }

    public UseRestriction getUseRestriction() {
        return useRestriction;
    }

    public void setUseRestriction(UseRestriction useRestriction) {
        this.useRestriction = useRestriction;
    }

    public String getTranslatedUseRestriction() {
        return translatedUseRestriction;
    }

    public void setTranslatedUseRestriction(String translatedUseRestriction) {
        this.translatedUseRestriction = translatedUseRestriction;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }
}