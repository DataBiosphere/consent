package org.broadinstitute.consent.http.models.datause;

import java.util.EnumSet;
import java.util.List;
import org.broadinstitute.consent.http.models.DataUse;

/** Classifies primary dataset Data Use fields without applying access-management policy. */
public final class DataUsePrimaryClassifier {

  private DataUsePrimaryClassifier() {}

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
