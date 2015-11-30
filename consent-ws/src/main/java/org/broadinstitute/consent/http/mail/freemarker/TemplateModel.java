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

    public String getElectionType() {
        return electionType;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getServerUrl() {
        return serverUrl;
    }

}
