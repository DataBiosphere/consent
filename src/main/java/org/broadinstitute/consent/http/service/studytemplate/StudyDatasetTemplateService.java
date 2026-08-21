package org.broadinstitute.consent.http.service.studytemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.InputStream;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.DraftStudyDataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.registration.template.StudyDatasetDraftReference;
import org.broadinstitute.consent.http.models.dto.registration.template.StudyTemplateValidationResult;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationResponse;
import org.broadinstitute.consent.http.service.DraftService;

/**
 * Turns an uploaded study template into a draft. Validation owns whether the template is usable;
 * this only decides what happens next, so an invalid template reaches no database write at all.
 */
public class StudyDatasetTemplateService {

  private final StudyTemplateValidationService validationService;
  private final DraftService draftService;
  private final ObjectMapper objectMapper;

  @Inject
  public StudyDatasetTemplateService(
      StudyTemplateValidationService validationService,
      DraftService draftService,
      ObjectMapper objectMapper) {
    this.validationService = validationService;
    this.draftService = draftService;
    this.objectMapper = objectMapper;
  }

  public TemplateValidationResponse validateAndCreateDraft(InputStream content, User user)
      throws SQLException, JsonProcessingException {
    StudyTemplateValidationResult result = validationService.validate(content);
    if (!result.valid()) {
      return TemplateValidationResponse.invalid(result.errors(), result.truncated());
    }

    // The injected mapper is the one the registration endpoint reads with, so the document the user
    // edits there is the one validated here.
    DraftInterface draft =
        new DraftStudyDataset(objectMapper.writeValueAsString(result.registration()), user);
    draftService.insertDraft(draft);
    return TemplateValidationResponse.valid(
        new StudyDatasetDraftReference(draft.getUUID().toString(), draft.getType().getValue()));
  }
}
