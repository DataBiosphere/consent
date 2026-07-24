package org.broadinstitute.consent.http.models.datause;

import java.util.EnumSet;
import java.util.List;

/**
 * Immutable classification of the primary categories present in a dataset Data Use. The category
 * list is retained for audit reporting and matcher diagnostics in addition to shape validation.
 */
public record DataUsePrimaryClassification(List<DataUsePrimaryCategory> categories) {

  public DataUsePrimaryClassification {
    EnumSet<DataUsePrimaryCategory> normalizedCategories =
        categories.isEmpty()
            ? EnumSet.noneOf(DataUsePrimaryCategory.class)
            : EnumSet.copyOf(categories);
    categories = List.copyOf(normalizedCategories);
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
