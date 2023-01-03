package org.broadinstitute.consent.http.mail.freemarker;

public class NewResearcherModel {

    private final String researcherName;
    private final String serverUrl;

    public NewResearcherModel(String researcherName, String serverUrl) {
        this.researcherName = researcherName;
        this.serverUrl = serverUrl;
    }

    public String getResearcherName() {
        return researcherName;
    }

    public String getServerUrl() {
        return serverUrl;
    }
}
