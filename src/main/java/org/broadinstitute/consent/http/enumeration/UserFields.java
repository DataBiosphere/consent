package org.broadinstitute.consent.http.enumeration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum UserFields {
  ERA_EXPIRATION_DATE("eraExpiration"),
  ERA_STATUS("eraAuthorized"),
  SELECTED_SIGNING_OFFICIAL_ID("selectedSigningOfficialId"),
  SUGGESTED_SIGNING_OFFICIAL("suggestedSigningOfficial"),
  DAA_ACCEPTANCE("daaAcceptance");

  private final String value;

  UserFields(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static List<String> getValues() {
    return Stream.of(UserFields.values()).map(UserFields::getValue).toList();
  }
}
