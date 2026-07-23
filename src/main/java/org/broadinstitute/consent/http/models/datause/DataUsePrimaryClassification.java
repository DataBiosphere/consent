package org.broadinstitute.consent.http.models.datause;

import java.util.List;

/** Immutable classification of the primary categories present in a dataset Data Use. */
public record DataUsePrimaryClassification(List<DataUsePrimaryCategory> categories) {

  public DataUsePrimaryClassification {
    categories = List.copyOf(categories);
  }

  public Shape shape() {
    return switch (categories.size()) {
      case 0 -> Shape.NONE;
      case 1 -> Shape.SINGLE;
      default -> Shape.MULTIPLE;
    };
  }

  public enum Shape {
    NONE,
    SINGLE,
    MULTIPLE
  }
}
