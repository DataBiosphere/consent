package org.broadinstitute.consent.http.models.datause;

import java.util.EnumSet;
import java.util.List;
import org.broadinstitute.consent.http.models.DataUse;

/** Classifies primary dataset Data Use fields without applying access-management policy. */
public final class DataUsePrimaryClassifier {

  private DataUsePrimaryClassifier() {}

  /**
   * Whether a Data Use has the single-primary shape DAC automation supports. {@code Shape.SINGLE}
   * also covers an Other-only primary category, which is non-canonical and must be excluded here to
   * match the abstention policy in {@code DataUseMatcherV5}.
   *
   * <p>Both the approval engine and dataset indexing gate on this before consulting a rule, so it
   * lives here rather than in either caller — the two must not be able to disagree about which
   * datasets automation can act on.
   */
  public static boolean hasCanonicalSinglePrimary(DataUse dataUse) {
    if (dataUse == null) {
      return false;
    }
    DataUsePrimaryClassification classification = classify(dataUse);
    return classification.shape() == DataUsePrimaryClassification.Shape.SINGLE
        && !classification.categories().contains(DataUsePrimaryCategory.OTHER);
  }

  public static DataUsePrimaryClassification classify(DataUse dataUse) {
    return classify(
        dataUse.getGeneralUse(),
        dataUse.getHmbResearch(),
        dataUse.getPopulationOriginsAncestry(),
        dataUse.getDiseaseRestrictions(),
        dataUse.getOther());
  }

  public static DataUsePrimaryClassification classify(
      Boolean generalUse,
      Boolean hmbResearch,
      Boolean populationOriginsAncestry,
      List<String> diseaseRestrictions,
      String other) {
    EnumSet<DataUsePrimaryCategory> categories = EnumSet.noneOf(DataUsePrimaryCategory.class);
    if (Boolean.TRUE.equals(generalUse)) {
      categories.add(DataUsePrimaryCategory.GRU);
    }
    if (Boolean.TRUE.equals(hmbResearch)) {
      categories.add(DataUsePrimaryCategory.HMB);
    }
    if (Boolean.TRUE.equals(populationOriginsAncestry)) {
      categories.add(DataUsePrimaryCategory.POA);
    }
    if (diseaseRestrictions != null && !diseaseRestrictions.isEmpty()) {
      categories.add(DataUsePrimaryCategory.DS);
    }
    if (other != null && !other.isBlank()) {
      categories.add(DataUsePrimaryCategory.OTHER);
    }
    return new DataUsePrimaryClassification(List.copyOf(categories));
  }
}
