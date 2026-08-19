package org.broadinstitute.consent.http.models.ontology;

import com.google.gson.annotations.Expose;

public class DataUseTerm {

  @Expose private String code;
  @Expose private String description;

  public DataUseTerm() {}

  public DataUseTerm(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
