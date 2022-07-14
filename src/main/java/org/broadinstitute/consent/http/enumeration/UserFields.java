package org.broadinstitute.consent.http.enumeration;

import java.util.ArrayList;
import java.util.List;

public enum UserFields {
  ERA_EXPIRATION_DATE("eraExpiration", false),
  ERA_STATUS("eraAuthorized", false),
  SELECTED_SIGNING_OFFICIAL_ID("selectedSigningOfficialId", false),
  SUGGESTED_SIGNING_OFFICIAL("suggestedSigningOfficial", false),
  SUGGESTED_INSTITUTION("suggestedInstitution", false);

  public static final String LIBRARY_CARDS = "libraryCards";
  public static final String LIBRARY_CARD_ENTRIES = "libraryCardEntries";
  private final String value;
  private final Boolean required;

  UserFields(String value, Boolean required) {
    this.value = value;
    this.required = required;
  }

  public String getValue() {
    return value;
  }

  public Boolean getRequired() {
    return required;
  }

  public static List<UserFields> getRequiredFields() {
    List<UserFields> requiredValues = new ArrayList<>();
    for (UserFields researcherField : UserFields.values()) {
      if (researcherField.getRequired()) {
        requiredValues.add(researcherField);
      }
    }
    return requiredValues;
  }

  public static Boolean containsValue(String value) {
    for (UserFields researcherField : UserFields.values()) {
      if (researcherField.getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
