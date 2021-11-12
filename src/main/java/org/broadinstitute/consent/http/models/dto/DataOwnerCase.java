package org.broadinstitute.consent.http.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.broadinstitute.consent.http.models.Dataset;

public class DataOwnerCase {
    @JsonProperty
    private String alias;

    @JsonProperty
    private Integer datasetId;

    @JsonProperty
    private String datasetName;

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

    public Integer getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
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
        this.alias = Dataset.parseAliasToIdentifier(alias);

    }
}
