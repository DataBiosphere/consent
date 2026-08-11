package org.broadinstitute.consent.http.enumeration;

import com.google.gson.annotations.SerializedName;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Which Signing Official authorization model applies to a dataset, derived from whether the
 * dataset's DAC has the REQUIRE_SO_DAR_APPROVAL automation rule enabled. Surfaced on indexed
 * datasets so clients do not have to resolve DAC rules themselves.
 */
public enum SoApprovalModel {
  /** The SO named in each DAR must approve that request before the DAC reviews it. */
  @SerializedName("PER_DAR")
  PER_DAR("PER_DAR"),

  /** The SO authorizes researchers in advance; no per-request SO approval is needed. */
  @SerializedName("PRE_AUTHORIZED")
  PRE_AUTHORIZED("PRE_AUTHORIZED");

  private final String value;

  public static List<String> getValues() {
    return Stream.of(SoApprovalModel.values())
        .map(SoApprovalModel::getValue)
        .collect(Collectors.toList());
  }

  SoApprovalModel(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static SoApprovalModel fromValue(String value) {
    Optional<SoApprovalModel> model =
        EnumSet.allOf(SoApprovalModel.class).stream()
            .filter(m -> m.getValue().equalsIgnoreCase(value))
            .findFirst();
    return model.orElse(null);
  }
}
