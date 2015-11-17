package org.genomebridge.consent.http.mail.freemarker;

public class NewDarRequestModel {

    /* This model works for templates: new-request. */

    private String serverUrl;

    public NewDarRequestModel(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
