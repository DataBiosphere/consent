package org.broadinstitute.consent.http.enumeration;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum FileCategory {

  @SerializedName("irbCollaborationLetter")
  IRB_COLLABORATION_LETTER("irbCollaborationLetter"),
  @SerializedName("dataUseLetter")
  DATA_USE_LETTER("dataUseLetter"),
  @SerializedName("alternativeDataSharingPlan")
  ALTERNATIVE_DATA_SHARING_PLAN("alternativeDataSharingPlan"),
  @SerializedName("nihInstitutionalCertification")
  NIH_INSTITUTIONAL_CERTIFICATION("nihInstitutionalCertification"),
  @SerializedName("dataAccessAgreement")
  DATA_ACCESS_AGREEMENT("dataAccessAgreement");

  private final String value;

  FileCategory(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static List<String> getValues() {
    return Stream.of(FileCategory.values()).map(FileCategory::getValue)
        .collect(Collectors.toList());
  }

  public static Boolean containsValue(String value) {
    for (FileCategory researcherField : FileCategory.values()) {
      if (researcherField.getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }

  public static FileCategory findValue(String value) {
    for (FileCategory cat : FileCategory.values()) {
      if (cat.getValue().equals(value)) {
        return cat;
      }
    }
    return null;
  }
}
