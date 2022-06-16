package org.broadinstitute.consent.http.models;

public class DarDataset {
    private String referenceId;
    private Integer datasetId;

    public DarDataset(String referenceId, Integer datasetId) {
        this.referenceId = referenceId;
        this.datasetId = datasetId;
    }

    public Integer getDatasetId() {
        return this.datasetId;
    }

    public void setId(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public String getReferenceId() {
        return this.referenceId;
    }

    public void setName(String referenceId) {
        this.referenceId = referenceId;
    }
}
