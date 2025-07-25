package org.broadinstitute.consent.http.enumeration;

import org.broadinstitute.consent.http.resources.Resource;

public enum VoteType {

  DAC("DAC"), FINAL("FINAL"), RADAR_APPROVE("RADAR_APPROVE"), AGREEMENT("AGREEMENT"), CHAIRPERSON(Resource.CHAIRPERSON);

  private final String value;

  VoteType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static String getValue(String value) {
    for (VoteType e : VoteType.values()) {
      if (e.getValue().equalsIgnoreCase(value)) {
        return e.getValue();
      }
    }
    return null;
  }


}
