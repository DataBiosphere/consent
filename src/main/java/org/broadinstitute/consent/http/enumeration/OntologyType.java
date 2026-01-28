package org.broadinstitute.consent.http.enumeration;

import java.util.Arrays;

public enum OntologyType {
  DUO("duo.owl"),
  DOID("doid.owl");

  final String fileName;

  OntologyType(String fileName) {
    this.fileName = fileName;
  }

  public String getFileName() {
    return fileName;
  }

  public static OntologyType getFromName(String name) {
    return Arrays.stream(values())
        .filter(v -> v.name().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }
}
