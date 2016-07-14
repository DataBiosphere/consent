package org.broadinstitute.consent.http.mail.freemarker;

public class CancelledDarModel {

    /* This model works for templates: cancelled-dar-request  */

    private String userType;

    private String entityId;

    private String serverUrl;

    public CancelledDarModel(String userType, String entityId, String serverUrl) {
        this.userType = userType;
        this.entityId = entityId;
        this.serverUrl = serverUrl;
    }

    public String getUserType() {
        return userType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

}