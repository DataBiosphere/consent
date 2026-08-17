package org.broadinstitute.consent.http.models.dto.registration.template;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One independently actionable study-template validation error. {@code row} is one-based and counts
 * the CSV header as row 1. Parser and conversion errors carry a row, and a column when a single
 * cell is at fault; violations that belong to the registration request as a whole carry neither.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemplateValidationError(Integer row, String column, String message) {

  public static TemplateValidationError of(String message) {
    return new TemplateValidationError(null, null, message);
  }

  public static TemplateValidationError at(int row, String message) {
    return new TemplateValidationError(row, null, message);
  }

  public static TemplateValidationError at(int row, String column, String message) {
    return new TemplateValidationError(row, column, message);
  }
}
