package org.broadinstitute.consent.http.enumeration;

public enum OrganizationType {
  FOR_PROFIT("For-Profit"),
  NON_PROFIT("Nonprofit");

  final String value;

  OrganizationType(String value) {
    this.value = value;
  }

  public static OrganizationType getOrganizationTypeFromString(String value) {
    for (OrganizationType e : OrganizationType.values()) {
      if (e.getValue().equalsIgnoreCase(value)) {
        return e;
      }
    }
    return null;
  }


  public String getValue() {
    return value;
  }
}
