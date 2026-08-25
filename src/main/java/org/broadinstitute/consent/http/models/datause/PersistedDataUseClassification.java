package org.broadinstitute.consent.http.models.datause;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classification of a persisted {@code dataset.data_use} value, covering the raw states a parsed
 * {@link DataUsePrimaryClassification} cannot express.
 *
 * <p>{@link #label()} is the only form safe to report or log: it names categories, never the Other
 * free text or the raw JSON behind them.
 */
public record PersistedDataUseClassification(State state, List<DataUsePrimaryCategory> categories) {

  public PersistedDataUseClassification {
    EnumSet<DataUsePrimaryCategory> normalizedCategories =
        categories.isEmpty()
            ? EnumSet.noneOf(DataUsePrimaryCategory.class)
            : EnumSet.copyOf(categories);
    categories = List.copyOf(normalizedCategories);
  }

  static PersistedDataUseClassification of(State state) {
    return new PersistedDataUseClassification(state, List.of());
  }

  /**
   * Whether {@code DataUsePrimaryValidator} would accept this shape. OPEN requires no primary and
   * everything else exactly one, so the answer depends on the dataset, not the value alone.
   */
  public boolean isCanonicalFor(boolean openAccess) {
    return openAccess ? state == State.NONE : state == State.SINGLE;
  }

  /** Whether V5 abstains rather than delegating to V4, per hasCanonicalSinglePrimary. */
  public boolean abstainsWhenMatched() {
    return state != State.SINGLE || categories.contains(DataUsePrimaryCategory.OTHER);
  }

  /** Redacted, stable label for reconciliation reporting, e.g. {@code MULTIPLE(HMB,OTHER)}. */
  public String label() {
    return categories.isEmpty()
        ? state.name()
        : "%s(%s)"
            .formatted(
                state.name(),
                categories.stream()
                    .map(DataUsePrimaryCategory::name)
                    .collect(Collectors.joining(",")));
  }

  public enum State {
    /** No stored value: either a database null or a JSON {@code null} literal. */
    NULL,
    EMPTY,
    UNPARSEABLE,
    NONE,
    SINGLE,
    MULTIPLE
  }
}
