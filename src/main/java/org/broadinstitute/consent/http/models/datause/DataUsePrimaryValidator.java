package org.broadinstitute.consent.http.models.datause;

import jakarta.ws.rs.BadRequestException;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryClassification.Shape;

/** Applies dataset access-management policy to canonical primary Data Use classifications. */
public final class DataUsePrimaryValidator {

  public static final String VALIDATION_MESSAGE =
      "Dataset must have exactly one primary data use (open access, or one of:"
          + " general research use, health/medical/biomedical, populations/origins/ancestry,"
          + " disease-specific, other)";

  private DataUsePrimaryValidator() {}

  public static void validate(DataUse dataUse, AccessManagement accessManagement) {
    if (dataUse == null) {
      throw new BadRequestException("Data Use is required");
    }
    validate(DataUsePrimaryClassifier.classify(dataUse), accessManagement);
  }

  public static void validate(
      DataUsePrimaryClassification classification, AccessManagement accessManagement) {
    boolean valid =
        AccessManagement.OPEN.equals(accessManagement)
            ? classification.shape() == Shape.NONE
            : classification.shape() == Shape.SINGLE;
    if (!valid) {
      throw new BadRequestException(VALIDATION_MESSAGE);
    }
  }
}
