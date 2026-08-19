package org.broadinstitute.consent.http.service.studytemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.DraftType;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.NihAnvilUse;
import org.broadinstitute.consent.http.models.dto.registration.ConsentGroupRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequestValidator;
import org.broadinstitute.consent.http.models.dto.registration.template.StudyTemplateValidationResult;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationError;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationResponse;
import org.broadinstitute.consent.http.service.DraftService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyDatasetTemplateServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock private StudyTemplateValidationService validationService;

  @Mock private DraftService draftService;

  private final User user = new User();

  private StudyDatasetTemplateService service() {
    return new StudyDatasetTemplateService(validationService, draftService);
  }

  @Test
  void testValidateAndCreateDraft_writesNothingForAnInvalidTemplate() throws Exception {
    List<TemplateValidationError> errors =
        List.of(TemplateValidationError.at(2, "value", "Study Name is required"));
    when(validationService.validate(any()))
        .thenReturn(StudyTemplateValidationResult.invalid(errors, true));

    TemplateValidationResponse response = service().validateAndCreateDraft(template(), user);

    assertFalse(response.valid());
    assertEquals(errors, response.errors());
    assertTrue(response.truncated());
    assertNull(response.draft());
    verify(draftService, never()).insertDraft(any());
  }

  @Test
  void testValidateAndCreateDraft_createsOneTypedDraftForAValidTemplate() throws Exception {
    when(validationService.validate(any()))
        .thenReturn(StudyTemplateValidationResult.valid(registration()));

    TemplateValidationResponse response = service().validateAndCreateDraft(template(), user);

    ArgumentCaptor<DraftInterface> captor = ArgumentCaptor.forClass(DraftInterface.class);
    verify(draftService).insertDraft(captor.capture());
    DraftInterface draft = captor.getValue();
    assertTrue(response.valid());
    assertEquals(List.of(), response.errors());
    assertEquals(draft.getUUID().toString(), response.draft().id());
    assertEquals(DraftType.STUDY_DATASET_SUBMISSION_V1.getValue(), response.draft().draftType());
  }

  @Test
  void testValidateAndCreateDraft_persistsADocumentTheRegistrationEndpointAccepts()
      throws Exception {
    when(validationService.validate(any()))
        .thenReturn(StudyTemplateValidationResult.valid(registration()));

    service().validateAndCreateDraft(template(), user);

    ArgumentCaptor<DraftInterface> captor = ArgumentCaptor.forClass(DraftInterface.class);
    verify(draftService).insertDraft(captor.capture());
    // The document a user edits and posts back is read the same way the registration endpoint
    // reads its payload, so a draft cannot be structurally unusable there.
    StudyRegistrationRequest persisted =
        OBJECT_MAPPER.readValue(captor.getValue().getJson(), StudyRegistrationRequest.class);
    assertEquals(List.of(), new StudyRegistrationRequestValidator().collectViolations(persisted));
    assertEquals("Synthetic Minimal Study", persisted.getStudyName());
    assertEquals(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL,
        persisted.getNihAnvilUse());
    assertEquals(
        AccessManagement.OPEN, persisted.getConsentGroups().getFirst().getAccessManagement());
    assertEquals("Synthetic Minimal Study", captor.getValue().getName());
  }

  @Test
  void testValidateAndCreateDraft_createsADistinctDraftPerRequest() throws Exception {
    when(validationService.validate(any()))
        .thenReturn(StudyTemplateValidationResult.valid(registration()));
    StudyDatasetTemplateService service = service();

    TemplateValidationResponse first = service.validateAndCreateDraft(template(), user);
    TemplateValidationResponse second = service.validateAndCreateDraft(template(), user);

    verify(draftService, times(2)).insertDraft(any());
    assertNotEquals(first.draft().id(), second.draft().id());
  }

  private static InputStream template() {
    return new ByteArrayInputStream("templateVersion".getBytes(StandardCharsets.UTF_8));
  }

  private static StudyRegistrationRequest registration() {
    StudyRegistrationRequest registration = new StudyRegistrationRequest();
    registration.setStudyName("Synthetic Minimal Study");
    registration.setStudyDescription("A synthetic study used only for contract tests.");
    registration.setDataTypes(List.of("Genomic"));
    registration.setPublicVisibility(true);
    registration.setNihAnvilUse(
        NihAnvilUse.I_AM_NOT_NHGRI_FUNDED_AND_DO_NOT_PLAN_TO_STORE_DATA_IN_AN_VIL);
    registration.setPiName("Synthetic Investigator");

    ConsentGroupRequest consentGroup = new ConsentGroupRequest();
    consentGroup.setConsentGroupName("Synthetic Open Dataset");
    consentGroup.setAccessManagement(AccessManagement.OPEN);
    consentGroup.setNumberOfParticipants(10);
    registration.setConsentGroups(List.of(consentGroup));
    return registration;
  }
}
