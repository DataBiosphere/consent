package org.broadinstitute.consent.http.mail.freemarker;

public class NewDAAUploadResearcherModel {

  private final String serverUrl;
  private final String dacName;
  private final String researcherUserName;
  private final String previousDaaName;
  private final String newDaaName;
  public NewDAAUploadResearcherModel(
      String serverUrl,
      String dacName,
      String researcherUserName,
      String previousDaaName,
      String newDaaName
  ) {
    this.serverUrl = serverUrl;
    this.dacName = dacName;
    this.researcherUserName = researcherUserName;
    this.previousDaaName = previousDaaName;
    this.newDaaName = newDaaName;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getDacName() {
    return dacName;
  }

  public String getResearcherUserName() {
    return researcherUserName;
  }

  public String getPreviousDaaName() {
    return previousDaaName;
  }

  public String getNewDaaName() {
    return newDaaName;
  }

}
