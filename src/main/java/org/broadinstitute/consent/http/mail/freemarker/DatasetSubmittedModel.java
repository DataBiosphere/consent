package org.broadinstitute.consent.http.mail.freemarker;

public class DatasetSubmittedModel {

  private String dacChairName;
  private String dataSubmitterName;
  private String datasetName;
  private String dacName;

  public DatasetSubmittedModel(String dacChairName, String dataSubmitterName, String datasetName,
      String dacName) {
    this.dacChairName = dacChairName;
    this.dataSubmitterName = dataSubmitterName;
    this.datasetName = datasetName;
    this.dacName = dacName;
  }

  public String getDacChairName() {
    return dacChairName;
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
}
