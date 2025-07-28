package org.broadinstitute.consent.http.models.dto;

import java.util.Objects;

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

  @Override
  public int hashCode() {
    return Objects.hash(identifier, name);
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!Objects.equals(this.identifier, ((DatasetMailDTO) o).identifier)) return false;
    return Objects.equals(this.name, ((DatasetMailDTO) o).name);
  }
}
