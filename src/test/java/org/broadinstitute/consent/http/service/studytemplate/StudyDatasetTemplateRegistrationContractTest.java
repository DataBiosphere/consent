package org.broadinstitute.consent.http.service.studytemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.resources.DatasetResource;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.DraftService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.TDRService;
import org.broadinstitute.consent.http.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The document a validated template persists is the one the study registration endpoint has to
 * accept once the user has reviewed it, so it is posted to that endpoint here rather than only
 * checked against the validator it shares.
 */
@ExtendWith(MockitoExtension.class)
class StudyDatasetTemplateRegistrationContractTest {

  private static final String MINIMAL_VALID = "fixtures/study-template/v1/valid/minimal-valid.csv";

  @Mock private DraftService draftService;
  @Mock private DatasetService datasetService;
  @Mock private DatasetRegistrationService datasetRegistrationService;
  @Mock private ElasticSearchService elasticSearchService;
  @Mock private TDRService tdrService;
  @Mock private UserService userService;
  @Mock private GCSService gcsService;

  private final User user = new User();
  private final DuosUser duosUser = new DuosUser(new AuthUser().setEmail("test@test.com"), user);

  @Test
  void testAValidatedTemplateDocumentIsAcceptedByTheRegistrationEndpoint() throws Exception {
    // One mapper for both sides, as the injector supplies: two of its own could not catch the
    // drift between them this contract exists to rule out.
    ObjectMapper objectMapper = new ObjectMapper();
    StudyDatasetTemplateService templateService =
        new StudyDatasetTemplateService(
            new StudyTemplateValidationService(), draftService, objectMapper);
    templateService.validateAndCreateDraft(fixture(), user);

    ArgumentCaptor<DraftInterface> captor = ArgumentCaptor.forClass(DraftInterface.class);
    verify(draftService).insertDraft(captor.capture());
    String document = captor.getValue().getJson();

    Study study = new Study();
    study.setStudyId(1);
    Dataset dataset = new Dataset();
    dataset.setStudyId(study.getStudyId());
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any()))
        .thenReturn(List.of(dataset));
    when(datasetService.findStudy(anyInt())).thenReturn(study);
    DatasetResource registrationResource =
        new DatasetResource(
            datasetService,
            userService,
            datasetRegistrationService,
            elasticSearchService,
            tdrService,
            gcsService,
            objectMapper);

    try (Response response =
        registrationResource.createDatasetRegistration(duosUser, null, document)) {
      // A 400 here would mean the document could not be deserialized or failed the endpoint's own
      // validation, which is the failure this contract exists to rule out.
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    }
  }

  private static InputStream fixture() {
    return StudyDatasetTemplateRegistrationContractTest.class
        .getClassLoader()
        .getResourceAsStream(MINIMAL_VALID);
  }
}
