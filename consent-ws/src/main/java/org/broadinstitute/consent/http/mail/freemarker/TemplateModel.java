package org.broadinstitute.consent.http.mail.freemarker;

public class TemplateModel {

    /* This model works for templates: collect and reminder. The placeholders are all the same. */

    private String userName;

    private String electionType;

    private String entityName;

    private String serverUrl;

    public TemplateModel(String user, String election, String entityName, String serverUrl) {
        this.userName = user;
        this.electionType = election;
        this.entityName = entityName;
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

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
