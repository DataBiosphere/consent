package org.genomebridge.consent.http.mail.freemarker;

public class TemplateModel {

    /* This model works for every template we have: collect, new-case and reminder. The placeholders are all the same. */

    private String userName;

    private String electionType;

    private String entityId;

    private String serverUrl;

    public TemplateModel(String user, String election, String entityId, String serverUrl) {
        this.userName = user;
        this.electionType = election;
        this.entityId = entityId;
        this.serverUrl = serverUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String user) {
        this.userName = user;
    }

    public String getElectionType() {
        return electionType;
    }

    public void setElectionType(String electionType) {
        this.electionType = electionType;
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
