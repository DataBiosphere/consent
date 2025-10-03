package org.broadinstitute.consent.http.models.draft;

public enum DataLocation {

  AN_VIL_WORKSPACE("AnVIL Workspace"),
  TERRA_WORKSPACE("Terra Workspace"),
  TDR_LOCATION("TDR Location"),
  NOT_DETERMINED("Not Determined");
  private final String value;

  DataLocation(String value) {
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
