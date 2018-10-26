package org.broadinstitute.consent.http.mail.freemarker;

import org.apache.commons.lang3.StringUtils;

public class DatasetOwnerListModel {

    private String ownerName;
    private String datasetName;
    private String datasetId;

    public DatasetOwnerListModel(String ownerName, String datasetName, String datasetId) {
        this.ownerName = ownerName;
        this.datasetName = datasetName;
        this.datasetId = StringUtils.isEmpty(datasetId) ? "--" : datasetId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getDatasetId() {
        return datasetId;
    }
}
