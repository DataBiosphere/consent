package org.broadinstitute.consent.http.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequestValidator;
import org.broadinstitute.consent.http.models.dto.registration.StudyUpdateRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyUpdateRequestValidator;
import org.broadinstitute.consent.http.util.ConsentLogger;

/**
 * Runs the new DTO-based registration validators in shadow (non-authoritative) mode alongside the
 * existing JSON Schema / manual validation, and logs agreement or discrepancy. This class never
 * throws back to its caller; the result it returns is informational only and must not affect the
 * authoritative validation outcome.
 */
public class RegistrationShadowValidator implements ConsentLogger {

  private final ObjectMapper mapper = new ObjectMapper();
  private final StudyRegistrationRequestValidator createValidator;
  private final StudyUpdateRequestValidator updateValidator;

  @Inject
  public RegistrationShadowValidator(DatasetService datasetService) {
    this.createValidator = new StudyRegistrationRequestValidator();
    this.updateValidator = new StudyUpdateRequestValidator(datasetService);
  }

  public record ValidationOutcome(boolean accepted, String message, long durationNanos) {

    public static ValidationOutcome accepted(long durationNanos) {
      return new ValidationOutcome(true, "", durationNanos);
    }

    public static ValidationOutcome rejected(String message, long durationNanos) {
      return new ValidationOutcome(false, message, durationNanos);
    }

    /**
     * Builds an accepted outcome, computing the duration from a {@link System#nanoTime()} timestamp
     * captured at the start of validation.
     */
    public static ValidationOutcome acceptedSince(long startNanos) {
      return accepted(System.nanoTime() - startNanos);
    }

    /**
     * Builds a rejected outcome, computing the duration from a {@link System#nanoTime()} timestamp
     * captured at the start of validation.
     */
    public static ValidationOutcome rejectedSince(String message, long startNanos) {
      return rejected(message, System.nanoTime() - startNanos);
    }
  }

  public record ComparisonResult(
      ValidationOutcome oldOutcome, ValidationOutcome newOutcome, boolean agree) {}

  /**
   * Compares the authoritative create-validation outcome against the new {@link
   * StudyRegistrationRequestValidator}. Deserializes {@code json} into a {@link
   * StudyRegistrationRequest} and runs the new validator, then logs the comparison.
   *
   * @param json the raw registration JSON submitted on create
   * @param oldOutcome the already-computed authoritative (JSON Schema) outcome
   * @return the comparison, for test assertions; callers should otherwise ignore it
   */
  public ComparisonResult compareCreate(String json, ValidationOutcome oldOutcome) {
    if (oldOutcome == null) {
      logWarn("[VALIDATOR_PARITY:create] shadow validation skipped: oldOutcome was null");
      return new ComparisonResult(null, null, false);
    }
    ValidationOutcome newOutcome =
        runTimed(
            () -> {
              StudyRegistrationRequest request =
                  mapper.readValue(json, StudyRegistrationRequest.class);
              createValidator.validate(request);
            });
    return logComparison("create", oldOutcome, newOutcome);
  }

  /**
   * Compares the authoritative update-validation outcome against the new {@link
   * StudyUpdateRequestValidator}. Deserializes {@code json} into a {@link StudyUpdateRequest} and
   * runs the new validator, then logs the comparison.
   *
   * @param json the raw registration JSON submitted on update
   * @param existingStudy the study being updated
   * @param oldOutcome the already-computed authoritative (manual) outcome
   * @return the comparison, for test assertions; callers should otherwise ignore it
   */
  public ComparisonResult compareUpdate(
      String json, Study existingStudy, ValidationOutcome oldOutcome) {
    if (oldOutcome == null) {
      logWarn("[VALIDATOR_PARITY:update] shadow validation skipped: oldOutcome was null");
      return new ComparisonResult(null, null, false);
    }
    ValidationOutcome newOutcome =
        runTimed(
            () -> {
              StudyUpdateRequest request = mapper.readValue(json, StudyUpdateRequest.class);
              updateValidator.validate(existingStudy, request);
            });
    return logComparison("update", oldOutcome, newOutcome);
  }

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run() throws JsonProcessingException;
  }

  private ValidationOutcome runTimed(ThrowingRunnable validation) {
    long start = System.nanoTime();
    try {
      validation.run();
      return ValidationOutcome.acceptedSince(start);
    } catch (BadRequestException e) {
      return ValidationOutcome.rejectedSince(e.getMessage(), start);
    } catch (JsonProcessingException e) {
      return ValidationOutcome.rejectedSince(
          "Unable to deserialize/validate: " + e.getMessage(), start);
    } catch (Exception e) {
      logWarn(
          "[VALIDATOR_PARITY] shadow validator threw unexpected exception: "
              + e.getClass().getSimpleName(),
          e);
      return ValidationOutcome.rejectedSince(
          "Shadow validator error: " + e.getClass().getSimpleName(), start);
    }
  }

  private ComparisonResult logComparison(
      String flow, ValidationOutcome oldOutcome, ValidationOutcome newOutcome) {
    boolean agree = oldOutcome.accepted() == newOutcome.accepted();
    String timing =
        "oldDurationMicros=%d newDurationMicros=%d"
            .formatted(oldOutcome.durationNanos() / 1_000, newOutcome.durationNanos() / 1_000);
    if (agree) {
      logInfo(
          "[VALIDATOR_PARITY:%s] AGREE accepted=%s %s"
              .formatted(flow, oldOutcome.accepted(), timing));
    } else {
      logWarn(
          "[VALIDATOR_PARITY:%s] DISAGREE old=%s new=%s oldMessage=%s newMessage=%s %s"
              .formatted(
                  flow,
                  oldOutcome.accepted() ? "ACCEPT" : "REJECT",
                  newOutcome.accepted() ? "ACCEPT" : "REJECT",
                  oldOutcome.message(),
                  newOutcome.message(),
                  timing));
    }
    return new ComparisonResult(oldOutcome, newOutcome, agree);
  }
}
