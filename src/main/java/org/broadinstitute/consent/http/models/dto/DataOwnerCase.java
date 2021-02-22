package org.broadinstitute.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.broadinstitute.consent.http.models.DataSet;

public class DataOwnerCase {
    @JsonProperty
    private String alias;

    @JsonProperty
    private Integer dataSetId;

    @JsonProperty
    private String dataSetName;

    @JsonProperty
    private String darCode;

    @JsonProperty
    private Integer voteId;

    @JsonProperty
    private String referenceId;

    @JsonProperty
    private Boolean alreadyVoted;

    @JsonProperty
    private Boolean hasConcerns;


    public DataOwnerCase() {
    }

    public Integer getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(Integer dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String getDarCode() {
        return darCode;
    }

    public void setDarCode(String darCode) {
        this.darCode = darCode;
    }

    public Integer getVoteId() {
        return voteId;
    }

    public void setVoteId(Integer voteId) {
        this.voteId = voteId;
    }

    public Boolean getAlreadyVoted() {
        return alreadyVoted;
    }

    public void setAlreadyVoted(Boolean alreadyVoted) {
        this.alreadyVoted = alreadyVoted;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Boolean getHasConcerns() {
        return hasConcerns;
    }

    public void setHasConcerns(Boolean hasConcerns) {
        this.hasConcerns = hasConcerns;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(Integer alias) {
        this.alias = DataSet.parseAliasToIdentifier(alias);

    }
}
