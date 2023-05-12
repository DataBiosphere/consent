package org.broadinstitute.consent.http.models.dto;

public class DatasetMailDTO {

  private String name;
  private String identifier;

  public DatasetMailDTO(String name, String identifier) {
    this.name = name;
    this.identifier = identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }
}
