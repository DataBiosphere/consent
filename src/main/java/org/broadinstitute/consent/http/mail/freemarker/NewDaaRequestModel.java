package org.broadinstitute.consent.http.mail.freemarker;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;

public class NewDaaRequestModel {

  private final String serverUrl;
  private final String daaName;
  private final String userName;
  private final String signingOfficialUserName;
  private final String dacName;

  public NewDaaRequestModel(
      String serverUrl,
      String daaName,
      String userName,
      String signingOfficialUserName,
      String dacName
  ) {
    this.serverUrl = serverUrl;
    this.daaName = daaName;
    this.userName = userName;
    this.signingOfficialUserName = signingOfficialUserName;
    this.dacName = dacName;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getDaaName() {
    return daaName;
  }

  public String getUserName() {
    return userName;
  }

  public String getSigningOfficialUserName() {
    return signingOfficialUserName;
  }

  public String getDacName() {
    return dacName;
  }

}
