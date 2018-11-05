package org.broadinstitute.consent.http.mail.freemarker;

import org.apache.commons.lang3.StringUtils;

public class DatasetOwnerListModel {

    private String ownerName;
    private String datasetName;
    private String objectId;

    public DatasetOwnerListModel(String ownerName, String datasetName, String objectId) {
        this.ownerName = ownerName;
        this.datasetName = datasetName;
        this.objectId = StringUtils.isEmpty(objectId) ? "--" : objectId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getObjectId() {
        return objectId;
    }
}
