package org.broadinstitute.consent.http.service.studytemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.cloudstore.GCSService;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.DraftType;
import org.broadinstitute.consent.http.models.DraftInterface;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequestValidator;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationResponse;
import org.broadinstitute.consent.http.service.DraftService;
import org.broadinstitute.consent.http.service.dao.DraftFileStorageServiceDAO;
import org.broadinstitute.consent.http.service.dao.DraftServiceDAO;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The seam between validation and the draft endpoints, against a real database: every part of this
 * is unit tested, but only here does one uploaded template become a row that the draft endpoints
 * can hand back.
 */
class StudyDatasetTemplateServiceDAOTest extends DAOTestHelper {

  private static final String MINIMAL_VALID = "fixtures/study-template/v1/valid/minimal-valid.csv";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private DraftService draftService;
  private StudyDatasetTemplateService templateService;

  @BeforeEach
  void beforeEachTestSetup() {
    GCSService gcsService = Mockito.mock(GCSService.class);
    DraftServiceDAO draftServiceDAO =
        new DraftServiceDAO(jdbi, new DraftFileStorageServiceDAO(jdbi, gcsService));
    draftService = new DraftService(jdbi, draftServiceDAO, gcsService);
    templateService =
        new StudyDatasetTemplateService(
            new StudyTemplateValidationService(), draftService, new ObjectMapper());
  }

  @Test
  void testValidTemplateBecomesADraftItsOwnerCanLoad() throws Exception {
    User owner = createUser();

    TemplateValidationResponse response = templateService.validateAndCreateDraft(fixture(), owner);

    assertTrue(response.valid(), response.errors().toString());
    assertEquals(DraftType.STUDY_DATASET_SUBMISSION_V1.getValue(), response.draft().draftType());

    UUID draftUUID = UUID.fromString(response.draft().id());
    DraftInterface persisted = draftService.getAuthorizedDraft(draftUUID, owner);
    assertEquals(owner.getUserId(), persisted.getCreateUser().getUserId());
    assertEquals("Synthetic Minimal Study", persisted.getName());

    // What the draft endpoint hands duos-ui: the document it must map, and the type it must check
    // before mapping it.
    JsonObject detail =
        GsonUtil.buildGson().fromJson(read(draftService.draftAsJson(persisted)), JsonObject.class);
    assertEquals(
        DraftType.STUDY_DATASET_SUBMISSION_V1.getValue(),
        detail.getAsJsonObject("meta").get("draftType").getAsString());

    StudyRegistrationRequest document =
        OBJECT_MAPPER.readValue(
            detail.getAsJsonObject("document").toString(), StudyRegistrationRequest.class);
    assertEquals(List.of(), new StudyRegistrationRequestValidator().collectViolations(document));
    assertEquals("Synthetic Minimal Study", document.getStudyName());
  }

  @Test
  void testADraftFromATemplateStaysWithItsOwner() throws Exception {
    User owner = createUser();
    User other = createUser();

    TemplateValidationResponse response = templateService.validateAndCreateDraft(fixture(), owner);

    UUID draftUUID = UUID.fromString(response.draft().id());
    assertThrows(
        NotAuthorizedException.class, () -> draftService.getAuthorizedDraft(draftUUID, other));
  }

  private static InputStream fixture() {
    return StudyDatasetTemplateServiceDAOTest.class
        .getClassLoader()
        .getResourceAsStream(MINIMAL_VALID);
  }

  private static String read(StreamingOutput output) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    output.write(bytes);
    return bytes.toString(StandardCharsets.UTF_8);
  }
}
