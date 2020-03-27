package org.broadinstitute.consent.http.models;

public class DatasetDetailEntry {

    String datasetId;
    String name;
    String objectId;

    public DatasetDetailEntry() {
    }

    public DatasetDetailEntry(String datasetId, String name, String objectId) {
        this.datasetId = datasetId;
        this.name = name;
        this.objectId = objectId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }
}
