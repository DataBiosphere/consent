package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.DraftType;
import org.broadinstitute.consent.http.exceptions.TemplateTooLargeException;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.registration.template.StudyDatasetDraftReference;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationResponse;
import org.broadinstitute.consent.http.service.studytemplate.StudyDatasetTemplateService;
import org.broadinstitute.consent.http.service.studytemplate.StudyTemplateValidationService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyDatasetTemplateResourceTest {

  @Mock private StudyDatasetTemplateService templateService;

  @Mock private DuosUser duosUser;

  @Mock private User user;

  @Mock private FormDataMultiPart multipart;

  private StudyDatasetTemplateResource resource;

  private void initResource() {
    resource = new StudyDatasetTemplateResource(templateService);
  }

  @Test
  void testValidateTemplateReturnsTheValidationResult() throws Exception {
    TemplateValidationResponse validated =
        TemplateValidationResponse.valid(
            new StudyDatasetDraftReference(
                UUID.randomUUID().toString(), DraftType.STUDY_DATASET_SUBMISSION_V1.getValue()));
    FormDataBodyPart part = filePart("1,study".getBytes(StandardCharsets.UTF_8));
    when(duosUser.getUser()).thenReturn(user);
    when(multipart.getFields("file")).thenReturn(List.of(part));
    when(templateService.validateAndCreateDraft(any(), any())).thenReturn(validated);
    initResource();

    Response response = resource.validateTemplate(duosUser, multipart);

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(validated, response.getEntity());
  }

  @ParameterizedTest
  @ValueSource(strings = {"no multipart", "no file part", "empty file part"})
  void testValidateTemplateRejectsAMissingFilePart(String shape) throws Exception {
    FormDataMultiPart body = multipart;
    switch (shape) {
      case "no multipart" -> body = null;
      case "no file part" -> when(multipart.getFields("file")).thenReturn(null);
      default -> when(multipart.getFields("file")).thenReturn(List.of());
    }
    initResource();

    Response response = resource.validateTemplate(duosUser, body);

    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus(), shape);
    verify(templateService, never()).validateAndCreateDraft(any(), any());
  }

  @Test
  void testValidateTemplateRejectsMoreThanOneFilePart() throws Exception {
    // Neither part is read: the request is unusable before their content matters.
    when(multipart.getFields("file"))
        .thenReturn(List.of(mock(FormDataBodyPart.class), mock(FormDataBodyPart.class)));
    initResource();

    Response response = resource.validateTemplate(duosUser, multipart);

    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    verify(templateService, never()).validateAndCreateDraft(any(), any());
  }

  @Test
  void testValidateTemplateReportsAnOversizedFileAsTooLarge() throws Exception {
    // The validator owns the limit; this pins that its refusal reaches the client as a 413.
    FormDataBodyPart part = filePart("1,study".getBytes(StandardCharsets.UTF_8));
    when(duosUser.getUser()).thenReturn(user);
    when(multipart.getFields("file")).thenReturn(List.of(part));
    when(templateService.validateAndCreateDraft(any(), any()))
        .thenThrow(new TemplateTooLargeException(StudyTemplateValidationService.TOO_LARGE_MESSAGE));
    initResource();

    Response response = resource.validateTemplate(duosUser, multipart);

    assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), response.getStatus());
    assertEquals(
        new Error(
            StudyTemplateValidationService.TOO_LARGE_MESSAGE,
            Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()),
        response.getEntity());
  }

  @Test
  void testValidateTemplateReportsAFailedDraftWrite() throws Exception {
    FormDataBodyPart part = filePart("1,study".getBytes(StandardCharsets.UTF_8));
    when(duosUser.getUser()).thenReturn(user);
    when(multipart.getFields("file")).thenReturn(List.of(part));
    when(templateService.validateAndCreateDraft(any(), any()))
        .thenThrow(new SQLException("insert failed"));
    initResource();

    Response response = resource.validateTemplate(duosUser, multipart);

    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testTemplateValidationKeepsTheTypedPathItsClientCallsAgainst() throws NoSuchMethodException {
    // duos-ui calls this URL literally, so a rename would compile, pass, and 404 the page.
    assertEquals(
        "api/draft/v1/study-dataset",
        StudyDatasetTemplateResource.class.getAnnotation(Path.class).value());
    assertEquals(
        "/template-validation",
        StudyDatasetTemplateResource.class
            .getDeclaredMethod("validateTemplate", DuosUser.class, FormDataMultiPart.class)
            .getAnnotation(Path.class)
            .value());
  }

  @Test
  void testEveryEndpointAdmitsTheRolesTheDraftEndpointsDo() {
    EndpointRoles.assertEveryEndpointAdmits(
        StudyDatasetTemplateResource.class,
        Set.of(Resource.ADMIN, Resource.CHAIRPERSON, Resource.DATASUBMITTER));
  }

  private static FormDataBodyPart filePart(byte[] content) {
    FormDataBodyPart part = mock(FormDataBodyPart.class);
    when(part.getValueAs(InputStream.class)).thenReturn(new ByteArrayInputStream(content));
    return part;
  }
}
