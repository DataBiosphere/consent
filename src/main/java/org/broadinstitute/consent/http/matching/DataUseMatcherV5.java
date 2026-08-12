package org.broadinstitute.consent.http.matching;

import com.google.inject.Inject;
import java.util.List;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryCategory;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryClassification;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryClassifier;
import org.broadinstitute.consent.http.models.matching.DataUseMatchResultType;

/** Applies canonical dataset-primary policy before delegating supported cases to V4. */
public class DataUseMatcherV5 {

  public static final String MISSING_PRIMARY_RATIONALE =
      "The dataset is missing a supported primary Data Use and requires manual review.";
  public static final String MULTIPLE_PRIMARY_RATIONALE =
      "The dataset has multiple primary Data Use categories and requires manual review.";
  public static final String OTHER_PRIMARY_RATIONALE =
      "The dataset has an Other primary Data Use and requires manual review.";

  private final DataUseMatcherV4 dataUseMatcherV4;

  @Inject
  public DataUseMatcherV5(DataUseMatcherV4 dataUseMatcherV4) {
    this.dataUseMatcherV4 = dataUseMatcherV4;
  }

  public MatchResult matchPurposeAndDatasetV5(DataUse purpose, DataUse dataset) {
    if (dataset == null) {
      return abstain(MISSING_PRIMARY_RATIONALE);
    }

    DataUsePrimaryClassification classification = DataUsePrimaryClassifier.classify(dataset);
    return switch (classification.shape()) {
      case NONE -> abstain(MISSING_PRIMARY_RATIONALE);
      case MULTIPLE -> abstain(MULTIPLE_PRIMARY_RATIONALE);
      case SINGLE ->
          classification.categories().contains(DataUsePrimaryCategory.OTHER)
              ? abstain(OTHER_PRIMARY_RATIONALE)
              : dataUseMatcherV4.matchPurposeAndDatasetV4(purpose, dataset);
    };
  }

  private MatchResult abstain(String rationale) {
    return MatchResult.from(DataUseMatchResultType.ABSTAIN, List.of(rationale));
  }
}
