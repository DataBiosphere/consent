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

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<String> getDisabledDatasets() {
        return disabledDatasets;
    }

    public void setDisabledDatasets(List<String> disabledDatasets) {
        this.disabledDatasets = disabledDatasets;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
