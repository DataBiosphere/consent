package org.broadinstitute.consent.http.mail.freemarker;

public class NewDarRequestModel {

    /* This model works for templates: new-request. */

    private String serverUrl;

    public NewDarRequestModel(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

}
