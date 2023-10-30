package org.broadinstitute.consent.http.enumeration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public enum UserFields {
  ERA_EXPIRATION_DATE("eraExpiration", false),
  ERA_STATUS("eraAuthorized", false),
  SELECTED_SIGNING_OFFICIAL_ID("selectedSigningOfficialId", false),
  SUGGESTED_SIGNING_OFFICIAL("suggestedSigningOfficial", false),
  SUGGESTED_INSTITUTION("suggestedInstitution", false),
  DAA_ACCEPTANCE("daaAcceptance", false);

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

  public static List<String> getValues() {
    return Stream.of(UserFields.values()).map(UserFields::getValue).toList();
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
