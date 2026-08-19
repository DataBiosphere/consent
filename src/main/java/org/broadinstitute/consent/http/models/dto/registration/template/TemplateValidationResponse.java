package org.broadinstitute.consent.http.models.dto.registration.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The wire response of the template-validation endpoint, discriminated by {@code valid}: a valid
 * template carries the draft it created and no errors, an invalid one carries its errors and no
 * draft. Both branches carry an error list so a caller can read it without checking which branch it
 * has.
 *
 * <p>A field with no value is absent rather than null, which is how a client tells an error with no
 * location from one on row 1. Responses are written by {@code JerseyGsonProvider}, which omits
 * nulls; the Jackson annotation says the same for any caller that maps this with an ObjectMapper.
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
