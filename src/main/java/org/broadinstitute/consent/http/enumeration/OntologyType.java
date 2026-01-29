package org.broadinstitute.consent.http.enumeration;

import java.util.Arrays;

/**
 * Enumeration representing different types of ontologies with their associated file names. Indexing
 * functionality requires that these files exist in a specific GCS location.
 */
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
