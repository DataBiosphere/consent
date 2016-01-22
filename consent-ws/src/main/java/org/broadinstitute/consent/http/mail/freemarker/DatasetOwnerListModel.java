package org.broadinstitute.consent.http.mail.freemarker;

public class DatasetOwnerListModel {

    private String ownerName;
    private String datasetName;
    private String datasetId;

    public DatasetOwnerListModel(String ownerName, String datasetName, String datasetId) {
        this.ownerName = ownerName;
        this.datasetName = datasetName;
        this.datasetId = datasetId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }
}