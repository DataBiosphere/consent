package org.broadinstitute.consent.http.models.dto.registration.template;

import java.util.List;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;

/**
 * The outcome of validating one uploaded study template. {@code registration} is populated only
 * when there are no errors; {@code truncated} reports that the error cap was reached and that the
 * file has further problems beyond the reported ones.
 */
public record StudyTemplateValidationResult(
    List<TemplateValidationError> errors,
    boolean truncated,
    StudyRegistrationRequest registration) {

  public StudyTemplateValidationResult {
    errors = List.copyOf(errors);
  }

  public static StudyTemplateValidationResult valid(StudyRegistrationRequest registration) {
    return new StudyTemplateValidationResult(List.of(), false, registration);
  }

  public static StudyTemplateValidationResult invalid(
      List<TemplateValidationError> errors, boolean truncated) {
    return new StudyTemplateValidationResult(errors, truncated, null);
  }

  /**
   * A template is valid only when it produced a registration. Deriving this from the registration
   * rather than from an empty error list means a result can never report itself valid with nothing
   * to hand back.
   */
  public boolean valid() {
    return registration != null;
  }
}
