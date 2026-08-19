package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.DraftType;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.registration.template.StudyDatasetDraftReference;
import org.broadinstitute.consent.http.models.dto.registration.template.TemplateValidationResponse;
import org.broadinstitute.consent.http.service.studytemplate.StudyDatasetTemplateService;
import org.broadinstitute.consent.http.service.studytemplate.StudyTemplateValidationService;
import org.glassfish.jersey.media.multipart.ContentDisposition;
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
  void testValidateTemplateRejectsAnOversizedFile() throws Exception {
    // A request failure rather than a validation result: the browser reports it the same way it
    // reports its own pre-check, and the producer has no cell to fix.
    byte[] oversized = new byte[StudyTemplateValidationService.MAX_TEMPLATE_BYTES + 1];
    FormDataBodyPart part = filePart(oversized);
    when(multipart.getFields("file")).thenReturn(List.of(part));
    initResource();

    Response response = resource.validateTemplate(duosUser, multipart);

    assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode(), response.getStatus());
    assertEquals(
        new Error(
            StudyTemplateValidationService.TOO_LARGE_MESSAGE,
            Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()),
        response.getEntity());
    verify(templateService, never()).validateAndCreateDraft(any(), any());
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
  void testEveryEndpointAdmitsTheRolesTheDraftEndpointsDo() {
    List<Method> endpoints =
        Arrays.stream(StudyDatasetTemplateResource.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(POST.class))
            .toList();

    assertFalse(endpoints.isEmpty());
    endpoints.forEach(
        endpoint -> {
          RolesAllowed roles = endpoint.getAnnotation(RolesAllowed.class);
          assertNotNull(roles, endpoint.getName());
          assertEquals(
              Set.of(Resource.ADMIN, Resource.CHAIRPERSON, Resource.DATASUBMITTER),
              Set.of(roles.value()),
              endpoint.getName());
        });
  }

  private static FormDataBodyPart filePart(byte[] content) {
    FormDataBodyPart part = mock(FormDataBodyPart.class);
    ContentDisposition disposition =
        ContentDisposition.type("form-data").fileName("template.csv").build();
    when(part.getContentDisposition()).thenReturn(disposition);
    when(part.getValueAs(InputStream.class)).thenReturn(new ByteArrayInputStream(content));
    return part;
  }
}
