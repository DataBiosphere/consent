package org.broadinstitute.consent.http.models.datause;

import com.google.gson.Gson;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseClassification.State;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * Classifies a raw persisted {@code dataset.data_use} value.
 *
 * <p>Distinct from {@link DataUsePrimaryClassifier}, which starts from a parsed {@link DataUse} and
 * so cannot tell a missing value from an unparseable one. Reconciliation has to count those
 * separately, and the row-mapping parser collapses both to null.
 */
public final class PersistedDataUseClassifier {

  private static final Gson GSON = GsonUtil.gsonBuilderWithAdapters().create();

  private PersistedDataUseClassifier() {}

  public static PersistedDataUseClassification classify(String rawDataUse) {
    if (rawDataUse == null) {
      return PersistedDataUseClassification.of(State.NULL);
    }
    if (rawDataUse.isBlank()) {
      return PersistedDataUseClassification.of(State.EMPTY);
    }

    DataUse dataUse;
    try {
      dataUse = GSON.fromJson(rawDataUse, DataUse.class);
    } catch (Exception _) {
      // Deliberately no cause or payload: the value can hold Other free text.
      return PersistedDataUseClassification.of(State.UNPARSEABLE);
    }
    if (dataUse == null) {
      return PersistedDataUseClassification.of(State.NULL);
    }
    return classify(dataUse);
  }

  public static PersistedDataUseClassification classify(DataUse dataUse) {
    DataUsePrimaryClassification classification = DataUsePrimaryClassifier.classify(dataUse);
    State state =
        switch (classification.shape()) {
          case NONE -> State.NONE;
          case SINGLE -> State.SINGLE;
          case MULTIPLE -> State.MULTIPLE;
        };
    return new PersistedDataUseClassification(state, classification.categories());
  }
}
