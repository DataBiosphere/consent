package org.broadinstitute.consent.http.mail.freemarker;

public class NewDAAUploadSOModel {

  private final String serverUrl;
  private final String dacName;
  private final String signingOfficialUserName;
  private final String previousDaaName;
  private final String newDaaName;
  public NewDAAUploadSOModel(
      String serverUrl,
      String dacName,
      String signingOfficialUserName,
      String previousDaaName,
      String newDaaName
  ) {
    this.serverUrl = serverUrl;
    this.dacName = dacName;
    this.signingOfficialUserName = signingOfficialUserName;
    this.previousDaaName = previousDaaName;
    this.newDaaName = newDaaName;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getDacName() {
    return dacName;
  }

  public String getSigningOfficialUserName() {
    return signingOfficialUserName;
  }

  public String getPreviousDaaName() {
    return previousDaaName;
  }

  public String getNewDaaName() {
    return newDaaName;
  }

}
