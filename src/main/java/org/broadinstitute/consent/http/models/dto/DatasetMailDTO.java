package org.broadinstitute.consent.http.models.dto;

import java.util.Objects;

public class DatasetMailDTO {

  private String name;
  private String identifier;
  private String dataLocation;

  public DatasetMailDTO(String name, String identifier, String dataLocation) {
    this.name = name;
    this.identifier = identifier;
    this.dataLocation = dataLocation;
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

  public String getDataLocation() {
    return dataLocation;
  }

  public void setDataLocation(String dataLocation) {
    this.dataLocation = dataLocation;
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, name, dataLocation);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!Objects.equals(this.identifier, ((DatasetMailDTO) o).identifier)) return false;
    if (!Objects.equals(this.name, ((DatasetMailDTO) o).name)) return false;
    return Objects.equals(this.dataLocation, ((DatasetMailDTO) o).dataLocation);
  }
}
