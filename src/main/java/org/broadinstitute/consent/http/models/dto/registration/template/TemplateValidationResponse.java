package org.broadinstitute.consent.http.models.dto.registration.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The wire response of the template-validation endpoint, discriminated by {@code valid}. Both
 * branches carry an error list, so a caller can read it without checking which branch it has, and
 * an absent field means no value: that is how a client tells an unlocated error from one on row 1.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemplateValidationResponse(
    boolean valid,
    List<TemplateValidationError> errors,
    boolean truncated,
    StudyDatasetDraftReference draft) {

  public TemplateValidationResponse {
    errors = List.copyOf(errors);
  }

  public static TemplateValidationResponse valid(StudyDatasetDraftReference draft) {
    return new TemplateValidationResponse(true, List.of(), false, draft);
  }

  public static TemplateValidationResponse invalid(
      List<TemplateValidationError> errors, boolean truncated) {
    return new TemplateValidationResponse(false, errors, truncated, null);
  }
}
