package org.broadinstitute.consent.http.mail.freemarker;

import org.apache.commons.lang3.StringUtils;

public class DataSetPIMailModel {

    private String objectId;
    private String name;
    private String datasetIdentifier;

    public DataSetPIMailModel(String objectId, String name, String datasetIdentifier) {
        this.objectId = StringUtils.isEmpty(objectId) ? "--" : objectId;
        this.name = name;
        this.datasetIdentifier = datasetIdentifier;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

    public String getDatasetIdentifier() {return datasetIdentifier; }

}