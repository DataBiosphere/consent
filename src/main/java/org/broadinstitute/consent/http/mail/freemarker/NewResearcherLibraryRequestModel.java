package org.broadinstitute.consent.http.mail.freemarker;

public class NewResearcherLibraryRequestModel {

  private final String researcherName;
  private final String serverUrl;

  public NewResearcherLibraryRequestModel(String researcherName, String serverUrl) {
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
