package org.broadinstitute.consent.http.mail.freemarker;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;

public class NewDarRequestModel {

  /* This model works for templates: new-request. */

  private final String serverUrl;
  private final String userName;
  private final Map<String, List<String>> dacDatasetGroups;
  private final String researcherUserName;
  private final String darID;



  public NewDarRequestModel(
      String serverUrl,
      String userName,
      Map<String, List<String>> dacDatasetGroups,
      String researcherUserName,
      String darID
  ) {
    this.serverUrl = serverUrl;
    this.userName = userName;
    this.dacDatasetGroups = dacDatasetGroups;
    this.researcherUserName = researcherUserName;
    this.darID = darID;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getUserName() {
    return userName;
  }

  public Map<String, List<String>> getDacDatasetGroups() {
    return dacDatasetGroups;
  }

  public String getResearcherUserName() {
    return researcherUserName;
  }

  public String getDarID() {
    return darID;
  }

}
