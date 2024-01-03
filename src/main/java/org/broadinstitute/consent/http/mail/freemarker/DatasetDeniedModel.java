package org.broadinstitute.consent.http.mail.freemarker;

public class DatasetDeniedModel {

  private String dataSubmitterName;
  private String datasetName;
  private String dacName;
  private String dacEmail;

  public DatasetDeniedModel(String dataSubmitterName, String datasetName, String dacName, String dacEmail) {
    this.dataSubmitterName = dataSubmitterName;
    this.datasetName = datasetName;
    this.dacName = dacName;
    this.dacEmail = dacEmail;
  }

  public String getDataSubmitterName() {
    return dataSubmitterName;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public String getDacName() {
    return dacName;
  }

  public String getDacEmail() {
    return dacEmail;
  }

}
