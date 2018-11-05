package org.broadinstitute.consent.http.mail.freemarker;

import org.apache.commons.lang3.StringUtils;

public class DataSetPIMailModel {

    private String objectId;
    private String name;

    public DataSetPIMailModel(String objectId, String name) {
        this.objectId = StringUtils.isEmpty(objectId) ? "--" : objectId;
        this.name = name;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

}