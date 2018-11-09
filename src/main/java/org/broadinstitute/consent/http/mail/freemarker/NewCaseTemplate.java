package org.broadinstitute.consent.http.mail.freemarker;

public class NewCaseTemplate {

     /* This model works for templates: new-case. */

    private String userName;

    private String electionType;

    private String entityName;

    private String serverUrl;

    public NewCaseTemplate(String userName, String election, String entityName, String serverUrl) {
        this.electionType = election;
        this.entityName = entityName;
        this.serverUrl = serverUrl;
        this.userName = userName;
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

    public String getUserName() {
        return userName;
    }

}