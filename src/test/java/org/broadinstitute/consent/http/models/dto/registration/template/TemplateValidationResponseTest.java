package org.broadinstitute.consent.http.models.dto.registration.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.broadinstitute.consent.http.enumeration.DraftType;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;

/**
 * The wire shape of the validation response, serialized the way the application serializes it:
 * every JSON entity goes through {@code JerseyGsonProvider}. A client distinguishes an error with
 * no location from one at row 1 by the absence of the field, so the omissions are the contract as
 * much as the values are.
 */
class TemplateValidationResponseTest {

  @Test
  void testInvalidResponseOmitsTheDraftAndUnknownLocations() {
    TemplateValidationResponse response =
        TemplateValidationResponse.invalid(
            List.of(
                TemplateValidationError.at(3, "value", "Study Name is required"),
                TemplateValidationError.at(4, "Row must have 6 columns but has 5"),
                TemplateValidationError.of("At least one Dataset is required")),
            true);

    assertEquals(
        """
        {"valid":false,"errors":[\
        {"row":3,"column":"value","message":"Study Name is required"},\
        {"row":4,"message":"Row must have 6 columns but has 5"},\
        {"message":"At least one Dataset is required"}],\
        "truncated":true}""",
        GsonUtil.getInstance().toJson(response));
  }

  @Test
  void testValidResponseCarriesAnEmptyErrorListAndTheTypedDraft() {
    TemplateValidationResponse response =
        TemplateValidationResponse.valid(
            new StudyDatasetDraftReference(
                "c2e4583a-20b9-4705-8280-e6a5753f10c9",
                DraftType.STUDY_DATASET_SUBMISSION_V1.getValue()));

    assertEquals(
        """
        {"valid":true,"errors":[],"truncated":false,\
        "draft":{"id":"c2e4583a-20b9-4705-8280-e6a5753f10c9",\
        "draftType":"StudyDatasetSubmissionV1"}}""",
        GsonUtil.getInstance().toJson(response));
  }
}
