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
 *
 * <p>Collection is bounded. A template can hold one broken row per line, so without a limit here a
 * file inside the 5 MiB cap would retain hundreds of thousands of errors to report a hundred.
 */
final class TemplateErrors {

  /**
   * Ten times the reported cap, so the reported errors are still the lowest-numbered rows of a file
   * whose problems arrive out of row order, while a pathological file costs a bounded list.
   */
  private static final int MAX_COLLECTED = 10 * StudyTemplateValidationService.MAX_ERRORS;

  private final List<TemplateValidationError> located = new ArrayList<>();
  private final List<TemplateValidationError> unlocated = new ArrayList<>();
  private int count;

  void at(int row, String column, String message) {
    add(located, TemplateValidationError.at(row, column, message));
  }

  void at(int row, String message) {
    add(located, TemplateValidationError.at(row, message));
  }

  void message(String message) {
    add(unlocated, TemplateValidationError.of(message));
  }

  /** Adds everything {@code other} collected, keeping each list's own order. */
  void merge(TemplateErrors other) {
    int dropped = other.count - other.located.size() - other.unlocated.size();
    addAll(located, other.located);
    addAll(unlocated, other.unlocated);
    count += dropped;
  }

  boolean isEmpty() {
    return count == 0;
  }

  /** Every error reported, including any beyond {@link #MAX_COLLECTED} that was not kept. */
  int count() {
    return count;
  }

  List<TemplateValidationError> toList() {
    return Stream.concat(
            located.stream().sorted(Comparator.comparing(TemplateValidationError::row)),
            unlocated.stream())
        .toList();
  }

  private void addAll(List<TemplateValidationError> sink, List<TemplateValidationError> errors) {
    errors.forEach(error -> add(sink, error));
  }

  private void add(List<TemplateValidationError> sink, TemplateValidationError error) {
    count++;
    if (located.size() + unlocated.size() < MAX_COLLECTED) {
      sink.add(error);
    }
  }
}
