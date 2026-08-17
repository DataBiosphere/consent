package org.broadinstitute.consent.http.service.studytemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationError;

/**
 * Accumulates template validation errors in the order the contract specifies: everything with a row
 * in row order, then the violations that belong to the request as a whole in the order the
 * registration validator reported them.
 */
final class TemplateErrors {

  private final List<TemplateValidationError> located = new ArrayList<>();
  private final List<TemplateValidationError> unlocated = new ArrayList<>();

  void at(int row, String column, String message) {
    located.add(TemplateValidationError.at(row, column, message));
  }

  void at(int row, String message) {
    located.add(TemplateValidationError.at(row, message));
  }

  void message(String message) {
    unlocated.add(TemplateValidationError.of(message));
  }

  boolean isEmpty() {
    return located.isEmpty() && unlocated.isEmpty();
  }

  List<TemplateValidationError> toList() {
    return Stream.concat(
            located.stream().sorted(Comparator.comparing(TemplateValidationError::row)),
            unlocated.stream())
        .toList();
  }
}
