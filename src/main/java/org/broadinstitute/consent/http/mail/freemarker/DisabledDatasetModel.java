package org.broadinstitute.consent.http.mail.freemarker;

import java.util.List;

public class DisabledDatasetModel {

    private String userName;

    private List<String> disabledDatasets;

    private String entityId;

    private String serverUrl;

    public DisabledDatasetModel(String userName, List<String> disabledDatasets, String entityId, String serverUrl) {
        this.userName = userName;
        this.disabledDatasets = disabledDatasets;
        this.entityId = entityId;
        this.serverUrl = serverUrl;
    }

    public String getUserName() {
        return userName;
    }

    public List<String> getDisabledDatasets() {
        return disabledDatasets;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

}
