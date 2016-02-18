package org.broadinstitute.consent.http.mail.freemarker;

public class DataSetPIMailModel {

    private String objectId;
    private String name;

    public DataSetPIMailModel(String objectId, String name) {
        this.objectId = objectId;
        this.name = name;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}