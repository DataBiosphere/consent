package org.broadinstitute.consent.http.models.draft;

public enum FileType {

  ARRAYS("Arrays"),
  GENOME("Genome"),
  EXOME("Exome"),
  SURVEY("Survey"),
  PHENOTYPE("Phenotype");
  private final String value;

  FileType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  public String value() {
    return this.value;
  }

}
