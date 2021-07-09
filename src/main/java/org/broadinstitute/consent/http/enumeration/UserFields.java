package org.broadinstitute.consent.http.enumeration;

import java.util.ArrayList;
import java.util.List;

public enum UserFields {
  @Deprecated //display name is stored on user
  PROFILE_NAME("profileName", false),
  ACADEMIC_BUSINESS_EMAIL("academicEmail", true),
  @Deprecated //instiution id is stored on user
  INSTITUTION("institution", false),
  DEPARTMENT("department", true),
  STREET_ADDRESS_1("address1", true),
  CITY("city", true),
  ZIP_POSTAL_CODE("zipcode", true),
  ARE_YOU_PRINCIPAL_INVESTIGATOR("isThePI", true),
  COUNTRY("country", true),
  DIVISION("division", false),
  STREET_ADDRESS_2("address2", false),
  STATE("state", false),
  PUBMED_ID("pubmedID", false),
  SCIENTIFIC_URL("scientificURL", false),
  DO_YOU_HAVE_PI("havePI", false),
  PI_NAME("piName", false),
  PI_EMAIL("piEmail", false),
  PI_eRA_COMMONS_ID("piERACommonsID", false),
  COMPLETED("completed", false),
  INVESTIGATOR("investigator", false),
  ERA_EXPIRATION_DATE("eraExpiration", false),
  ERA_STATUS("eraAuthorized", false),
  @Deprecated
  ERA_USERNAME("nihUsername", false),
  LINKEDIN_PROFILE("linkedIn", false),
  RESEARCHER_GATE("researcherGate", false),
  ORCID("orcid", false),
  CHECK_NOTIFICATIONS("checkNotifications", false);

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
