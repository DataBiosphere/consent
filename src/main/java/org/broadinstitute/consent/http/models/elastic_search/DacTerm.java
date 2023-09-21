package org.broadinstitute.consent.http.models.elastic_search;

public class DacTerm {

  private Integer dacId;

  private String dacName;

  public Integer getDacId() {
    return dacId;
  }

  public void setDacId(Integer dacId) {
    this.dacId = dacId;
  }

  public String getDacName() {
    return dacName;
  }

  public void setDacName(String dacName) {
    this.dacName = dacName;
  }
}
