package org.broadinstitute.consent.http.enumeration;

import com.google.gson.annotations.SerializedName;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum DraftType {
  @SerializedName("StudyDatasetSubmissionV1")
  STUDY_DATASET_SUBMISSION_V1("StudyDatasetSubmissionV1");

  private final String value;

  public static List<String> getValues() {
    return Stream.of(DraftType.values()).map(DraftType::getValue).collect(Collectors.toList());
  }

  DraftType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static DraftType fromValue(String value) {
    Optional<DraftType> type =
        EnumSet.allOf(DraftType.class).stream()
            .filter(t -> t.getValue().equalsIgnoreCase(value))
            .findFirst();
    return type.orElse(null);
  }
}
